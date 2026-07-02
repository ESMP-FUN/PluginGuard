package io.github.darkstarworks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks plugin-enumeration probe attempts on a per-player sliding window and raises
 * alerts when the weighted score crosses a configurable threshold.
 *
 * Lock-free on the read/write path used by event handlers:
 *  - The per-player map is a [ConcurrentHashMap].
 *  - Each per-player tracker is only mutated inside its own [synchronized] block; trackers
 *    are independent, so contention is bounded to events fired for the same player on the
 *    same region thread.
 *
 * All I/O (file appends, broadcast to admins) is dispatched off the calling region thread
 * via Paper / Folia's async and global-region schedulers, so the event handler itself
 * stays cheap.
 */
class ProbeDetector(private val plugin: PluginGuard) {

    enum class Category(val weight: Int, val displayName: String) {
        HIGH(3, "high"),
        MEDIUM(2, "medium"),
        LOW(1, "low"),
        HONEYPOT(5, "honeypot"),
    }

    private data class Hit(val timestampMs: Long, val weight: Int)

    private class Tracker {
        // Bounded ring of recent hits. Synchronized on the tracker instance.
        val hits: ArrayDeque<Hit> = ArrayDeque()
        var lastAlertMs: Long = 0L
        // Last few category labels seen, for the alert message.
        val recentLabels: ArrayDeque<String> = ArrayDeque()
    }

    private val trackers = ConcurrentHashMap<UUID, Tracker>()

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun forgetPlayer(uuid: UUID) {
        trackers.remove(uuid)
    }

    fun forgetAll() {
        trackers.clear()
    }

    /**
     * Record a probe attempt by [player] with the given [category] and matched [label]
     * (e.g. "/pl", "bukkit:plugins", "honeypot:staffchat"). Returns nothing — alerts and
     * file writes are dispatched asynchronously.
     */
    fun record(player: Player, category: Category, label: String) {
        val s = plugin.currentSettings()
        if (!s.loggingEnabled) return

        // Console log of individual probe, opt-in.
        if (s.logIndividualProbes) {
            plugin.logger.info("Probe: ${player.name} (${player.uniqueId}) -> [$label] ${category.displayName}")
        }

        // File log of individual probe, opt-in. Dispatched off-thread.
        if (s.logToFile) {
            val line = buildString {
                append(timestampFormatter.format(Instant.now()))
                append(' ')
                append(player.name)
                append(' ').append(player.uniqueId)
                append(" [").append(category.displayName).append("] ")
                append(label)
            }
            appendToFileAsync(line)
        }

        if (!s.detectionEnabled) return

        val tracker = trackers.computeIfAbsent(player.uniqueId) { Tracker() }
        val now = System.currentTimeMillis()
        val windowMs = s.detectionWindowSeconds * 1000L
        val cooldownMs = s.detectionAlertCooldownSeconds * 1000L

        var labelsSnapshot: List<String> = emptyList()
        var ageSec = 0
        val (score, triggered) = synchronized(tracker) {
            // Prune expired hits.
            val cutoff = now - windowMs
            while (tracker.hits.isNotEmpty() && tracker.hits.peekFirst().timestampMs < cutoff) {
                tracker.hits.pollFirst()
            }
            tracker.hits.addLast(Hit(now, category.weight))
            // Track recent labels for alert text (cap at 6 to keep the message readable).
            tracker.recentLabels.addLast(label)
            while (tracker.recentLabels.size > 6) tracker.recentLabels.pollFirst()

            val total = tracker.hits.sumOf { it.weight }
            val fire = total >= s.detectionScoreThreshold &&
                    (tracker.lastAlertMs == 0L || now - tracker.lastAlertMs >= cooldownMs)
            if (fire) {
                tracker.lastAlertMs = now
                // Snapshot alert details while still holding the lock — reading the deques
                // afterwards would race with concurrent record() calls for the same player.
                labelsSnapshot = tracker.recentLabels.toList()
                ageSec = ((now - (tracker.hits.peekFirst()?.timestampMs ?: now)) / 1000L).toInt()
            }
            total to fire
        }

        if (triggered) {
            dispatchAlert(player, score, ageSec, labelsSnapshot, s.notifyPermission)
        }
    }

    private fun dispatchAlert(player: Player, score: Int, ageSec: Int, labels: List<String>, notifyPermission: String) {
        val labelText = labels.joinToString(", ")
        val consoleLine = "Probe detector tripped: ${player.name} (${player.uniqueId}) score=$score in ${ageSec}s [$labelText]"
        plugin.logger.warning(consoleLine)

        // Broadcast to online admins on the global region thread (Folia-safe; on Paper this
        // is the main thread shim).
        plugin.server.globalRegionScheduler.execute(plugin) {
            val message = Component.text("[PluginGuard] ", NamedTextColor.GOLD)
                .append(Component.text(player.name, NamedTextColor.YELLOW))
                .append(Component.text(" tripped the probe detector (score $score in ${ageSec}s): ", NamedTextColor.GRAY))
                .append(Component.text(labelText, NamedTextColor.WHITE))
            for (online in plugin.server.onlinePlayers) {
                if (online.hasPermission(notifyPermission)) {
                    online.sendMessage(message)
                }
            }
        }
    }

    private fun appendToFileAsync(line: String) {
        plugin.server.asyncScheduler.runNow(plugin) {
            try {
                val dir: Path = plugin.dataFolder.toPath()
                Files.createDirectories(dir)
                val file = dir.resolve("probes.log")
                Files.writeString(
                    file,
                    line + System.lineSeparator(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                )
            } catch (e: IOException) {
                plugin.logger.warning("Failed to write probes.log entry: ${e.message}")
            }
        }
    }
}
