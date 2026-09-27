package io.github.darkstarworks

import org.bukkit.entity.Player
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Scores each player's probes over a sliding window and warns staff past the threshold. */
class ProbeDetector(private val plugin: PluginGuard) {

    enum class Category(val weight: Int) { HIGH(3), MEDIUM(2), LOW(1), HONEYPOT(5) }

    private data class Hit(val timestampMs: Long, val weight: Int)

    private class Tracker {
        val hits: ArrayDeque<Hit> = ArrayDeque()
        var lastAlertMs: Long = 0L
        val recentLabels: ArrayDeque<String> = ArrayDeque()
    }

    private val trackers = ConcurrentHashMap<UUID, Tracker>()
    private val fileLock = Any()
    private val http: HttpClient by lazy {
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    private val timestampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    fun forgetPlayer(uuid: UUID) {
        trackers.remove(uuid)
    }

    fun forgetAll() {
        trackers.clear()
    }

    /** Records a probe by [player]; [label] is what they typed, e.g. "/pl" or "honeypot:opme". */
    fun record(player: Player, category: Category, label: String) {
        val s = plugin.currentSettings()

        if (s.logIndividualProbes) {
            plugin.logger.info("${player.name} tried $label (${category.weight} points)")
        }

        if (s.logToFile) {
            val line = "${timestampFormatter.format(Instant.now())} ${player.name} ${player.uniqueId} [${category.name.lowercase()}] $label"
            Platform.runAsync(plugin) { appendToFile(line, s.logMaxSizeBytes) }
        }

        if (!s.detectionEnabled) return

        val tracker = trackers.computeIfAbsent(player.uniqueId) { Tracker() }
        val now = System.currentTimeMillis()
        val windowMs = s.detectionWindowSeconds * 1000L
        val cooldownMs = s.detectionAlertCooldownSeconds * 1000L

        var labelsSnapshot: List<String> = emptyList()
        var ageSec = 0
        val (score, triggered) = synchronized(tracker) {
            val cutoff = now - windowMs
            while (tracker.hits.isNotEmpty() && tracker.hits.peekFirst().timestampMs < cutoff) {
                tracker.hits.pollFirst()
            }
            tracker.hits.addLast(Hit(now, category.weight))
            tracker.recentLabels.addLast(label)
            while (tracker.recentLabels.size > 6) tracker.recentLabels.pollFirst()

            val total = tracker.hits.sumOf { it.weight }
            val fire = total >= s.detectionScoreThreshold &&
                (tracker.lastAlertMs == 0L || now - tracker.lastAlertMs >= cooldownMs)
            if (fire) {
                tracker.lastAlertMs = now
                labelsSnapshot = tracker.recentLabels.toList()
                ageSec = ((now - (tracker.hits.peekFirst()?.timestampMs ?: now)) / 1000L).toInt()
            }
            total to fire
        }

        if (triggered) dispatchAlert(player, score, ageSec, labelsSnapshot, s)
    }

    private fun dispatchAlert(player: Player, score: Int, ageSec: Int, labels: List<String>, s: PluginGuard.Settings) {
        val labelText = labels.joinToString(", ")
        plugin.logger.warning("${player.name} looks like they are searching for your plugins (score $score in $ageSec seconds): $labelText")

        val name = player.name
        val uuid = player.uniqueId
        Platform.runGlobal(plugin) {
            for (online in plugin.server.onlinePlayers) {
                if (online.hasPermission(s.notifyPermission)) {
                    plugin.messages.send(online, "alert", "player" to name, "score" to score, "seconds" to ageSec, "tried" to labelText)
                }
            }
            // Only name characters are passed on, so a name can never add arguments to a command.
            val safeName = name.filter { it.isLetterOrDigit() || it == '_' || it == '.' }
            for (template in s.alertCommands) {
                val cmd = template.removePrefix("/")
                    .replace("%player%", safeName)
                    .replace("%uuid%", uuid.toString())
                    .replace("%score%", score.toString())
                try {
                    plugin.server.dispatchCommand(plugin.server.consoleSender, cmd)
                } catch (e: Exception) {
                    plugin.logger.warning("The alert command \"$cmd\" didn't work: ${e.message}")
                }
            }
        }

        if (s.discordWebhook.isNotEmpty()) {
            Platform.runAsync(plugin) { postWebhook(s.discordWebhook, name, uuid, score, ageSec, labelText) }
        }
    }

    private fun postWebhook(url: String, name: String, uuid: UUID, score: Int, ageSec: Int, labels: String) {
        val content = "**$name** (`$uuid`) looks like they are searching for your plugins: score $score in $ageSec seconds.\nTried: `${labels.replace('`', '\'')}`"
        // allowed_mentions stops a typed "@everyone" in a command from pinging the channel.
        val body = """{"username":"PluginGuard","content":${jsonString(content)},"allowed_mentions":{"parse":[]}}"""
        try {
            val req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
            val res = http.send(req, HttpResponse.BodyHandlers.discarding())
            if (res.statusCode() !in 200..299) {
                plugin.logger.warning("Discord refused the warning (HTTP ${res.statusCode()}). Check discord-webhook in config.yml.")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Could not send the warning to Discord: ${e.message}")
        }
    }

    private fun jsonString(s: String): String = buildString {
        append('"')
        for (c in s) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c == '\n' -> append("\\n")
            c < ' ' -> append(String.format("\\u%04x", c.code))
            else -> append(c)
        }
        append('"')
    }

    private fun appendToFile(line: String, maxBytes: Long) {
        synchronized(fileLock) {
            try {
                val dir: Path = plugin.dataFolder.toPath()
                Files.createDirectories(dir)
                val file = dir.resolve("probes.log")
                if (maxBytes > 0 && Files.exists(file) && Files.size(file) >= maxBytes) {
                    Files.move(file, dir.resolve("probes.old.log"), StandardCopyOption.REPLACE_EXISTING)
                }
                Files.writeString(file, line + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND)
            } catch (e: IOException) {
                plugin.logger.warning("Could not write to probes.log: ${e.message}")
            }
        }
    }
}
