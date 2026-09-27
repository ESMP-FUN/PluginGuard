package io.github.darkstarworks

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.TabCompleteEvent
import org.bukkit.plugin.java.JavaPlugin

class PluginGuard : JavaPlugin(), Listener {

    // Replaced whole on reload, so handlers on different Folia threads never see a half-loaded mix.
    data class Settings(
        val hideMode: String,
        val fakePlugins: List<String>,
        val bypassPermission: String,
        val protectedCommands: Set<String>,
        val commonPluginCommands: Set<String>,
        val honeypotCommands: Set<String>,
        val fakeServerBrand: String,
        val blockBukkitCommands: Boolean,
        val redirectBukkitCommands: Boolean,
        val hideTabCompletion: Boolean,
        val blockUnknownCommands: Boolean,
        val hideServerBrand: Boolean,
        val blockCommonPluginCommands: Boolean,
        val blockNamespacedCommands: Boolean,
        val aggressiveMode: Boolean,
        val hidePluginChannels: Boolean,
        val allowedPluginChannels: Set<String>,
        val logToFile: Boolean,
        val logMaxSizeBytes: Long,
        val logIndividualProbes: Boolean,
        val detectionEnabled: Boolean,
        val detectionScoreThreshold: Int,
        val detectionWindowSeconds: Int,
        val detectionAlertCooldownSeconds: Int,
        val notifyPermission: String,
        val alertCommands: List<String>,
        val discordWebhook: String,
    )

    @Volatile
    private lateinit var settings: Settings

    private val detector = ProbeDetector(this)
    private val packetSpoofer = PacketSpoofer(this)
    val messages = Messages(this)

    fun currentSettings(): Settings = settings

    override fun onEnable() {
        saveDefaultConfig()
        messages.reload()
        settings = loadSettings()
        server.pluginManager.registerEvents(this, this)
        if (Platform.isPaper) {
            server.pluginManager.registerEvents(PaperListener(this), this)
        } else {
            logger.warning("This server is not Paper, so the server brand, plugin channels and query answer can't be hidden.")
        }
        packetSpoofer.enable()
        io.github.darkstarworks.pluginpulse.PluginPulse.bootstrap(this)
        logger.info("Hiding ${server.pluginManager.plugins.size} plugins from players.")
    }

    override fun onDisable() {
        detector.forgetAll()
        packetSpoofer.disable()
        io.github.darkstarworks.pluginpulse.PluginPulse.shutdown(this)
    }

    private fun loadSettings(): Settings {
        reloadConfig()
        val hideMode = config.getString("hide-mode", "unknown-command")!!.lowercase()
        val webhook = config.getString("logging.detection.discord-webhook", "")!!.trim()
        return Settings(
            hideMode = if (hideMode in HIDE_MODES) hideMode else {
                logger.warning("hide-mode \"$hideMode\" in config.yml is not an option (use ${HIDE_MODES.joinToString(", ")}). Using unknown-command for now.")
                "unknown-command"
            },
            fakePlugins = config.getStringList("fake-plugins"),
            bypassPermission = config.getString("bypass-permission", "pluginguard.bypass")!!,
            protectedCommands = config.getStringList("protected-commands").mapTo(HashSet()) { it.lowercase() },
            commonPluginCommands = config.getStringList("common-plugin-commands").mapTo(HashSet()) { it.lowercase() },
            honeypotCommands = config.getStringList("honeypot-commands").mapTo(HashSet()) { it.removePrefix("/").lowercase() },
            fakeServerBrand = config.getString("fake-server-brand", "vanilla")!!,
            blockBukkitCommands = config.getBoolean("block-bukkit-commands", true),
            redirectBukkitCommands = config.getBoolean("redirect-bukkit-commands", false),
            hideTabCompletion = config.getBoolean("hide-tab-completion", true),
            blockUnknownCommands = config.getBoolean("block-unknown-commands", true),
            hideServerBrand = config.getBoolean("hide-server-brand", true),
            blockCommonPluginCommands = config.getBoolean("block-common-plugin-commands", true),
            blockNamespacedCommands = config.getBoolean("block-namespaced-commands", true),
            aggressiveMode = config.getBoolean("aggressive-mode", false),
            hidePluginChannels = config.getBoolean("hide-plugin-channels", true),
            allowedPluginChannels = config.getStringList("allowed-plugin-channels").mapTo(HashSet()) { it.trim().lowercase() },
            logToFile = config.getBoolean("logging.log-to-file", false),
            logMaxSizeBytes = config.getLong("logging.log-max-size-mb", 5L).coerceAtLeast(0L) * 1024L * 1024L,
            logIndividualProbes = config.getBoolean("logging.log-individual-probes", false),
            detectionEnabled = config.getBoolean("logging.detection.enabled", true),
            detectionScoreThreshold = config.getInt("logging.detection.score-threshold", 5).coerceAtLeast(1),
            detectionWindowSeconds = config.getInt("logging.detection.window-seconds", 60).coerceAtLeast(1),
            detectionAlertCooldownSeconds = config.getInt("logging.detection.alert-cooldown-seconds", 300).coerceAtLeast(0),
            notifyPermission = config.getString("logging.detection.notify-permission", "pluginguard.alerts")!!,
            alertCommands = config.getStringList("logging.detection.alert-commands").filter { it.isNotBlank() },
            discordWebhook = if (webhook.isEmpty() || webhook.startsWith("https://")) webhook else {
                logger.warning("discord-webhook in config.yml must start with https://. Discord alerts are off until it does.")
                ""
            },
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val s = settings
        if (player.hasPermission(s.bypassPermission)) return

        val msg = event.message
        val baseCommand = baseCommandOf(msg) ?: return
        val cleanCommand = stripVanillaNamespace(baseCommand)

        // Honeypot first: the highest-signal probe, and never forwarded.
        if (cleanCommand in s.honeypotCommands || baseCommand in s.honeypotCommands) {
            event.isCancelled = true
            messages.unknownCommand(player, msg)
            detector.record(player, ProbeDetector.Category.HONEYPOT, "honeypot:$baseCommand")
            return
        }

        when {
            baseCommand in s.protectedCommands || cleanCommand in s.protectedCommands -> {
                event.isCancelled = true
                if (baseCommand.startsWith("bukkit:") && s.redirectBukkitCommands) {
                    handlePluginsCommand(player, msg, s)
                } else {
                    handleProtectedCommand(player, cleanCommand, msg, s)
                }
                // /help and /? are typed legitimately far too often to count as a probe.
                if (cleanCommand != "help" && cleanCommand != "?") {
                    val cat = if (baseCommand != cleanCommand || cleanCommand == "icanhasbukkit")
                        ProbeDetector.Category.HIGH
                    else
                        ProbeDetector.Category.MEDIUM
                    detector.record(player, cat, "/$baseCommand")
                }
            }
            baseCommand != cleanCommand && s.blockBukkitCommands -> {
                event.isCancelled = true
                messages.unknownCommand(player, msg)
                detector.record(player, ProbeDetector.Category.HIGH, baseCommand)
            }
            // The namespace of /essentials:home is the plugin's name, so it confirms the plugin
            // even when the bare alias is hidden.
            s.blockNamespacedCommands && ':' in baseCommand -> {
                event.isCancelled = true
                messages.unknownCommand(player, msg)
                detector.record(player, ProbeDetector.Category.HIGH, baseCommand)
            }
            baseCommand in s.commonPluginCommands && s.blockCommonPluginCommands -> {
                event.isCancelled = true
                messages.unknownCommand(player, msg)
                detector.record(player, ProbeDetector.Category.LOW, "/$baseCommand")
            }
            s.aggressiveMode && !player.hasPermission("$baseCommand.use") -> {
                val cmd = server.getPluginCommand(baseCommand)
                if (cmd != null && cmd.plugin != this) {
                    event.isCancelled = true
                    messages.unknownCommand(player, msg)
                }
            }
            // A plugin's own "no permission" reply confirms it exists. Not a probe: legitimate
            // players hit permission walls all the time.
            s.blockUnknownCommands -> {
                val cmd = server.getPluginCommand(baseCommand)
                if (cmd != null && cmd.plugin != this && !cmd.testPermissionSilent(player)) {
                    event.isCancelled = true
                    messages.unknownCommand(player, msg)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommandSend(event: PlayerCommandSendEvent) {
        val s = settings
        if (!s.hideTabCompletion) return
        if (event.player.hasPermission(s.bypassPermission)) return
        event.commands.removeIf { isHiddenCommand(event.player, it.lowercase(), s) }
    }

    // Argument suggestions ("/version " lists every plugin). A modified client can ask even for a
    // hidden command. Paper sends these through AsyncTabCompleteEvent instead; this covers Spigot.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onTabComplete(event: TabCompleteEvent) {
        if (hidesSuggestionsFor(event.sender, event.buffer)) {
            event.completions = mutableListOf()
            event.isCancelled = true
        }
    }

    fun hidesSuggestionsFor(sender: CommandSender, buffer: String): Boolean {
        if (sender !is Player) return false
        val s = settings
        if (!s.hideTabCompletion || sender.hasPermission(s.bypassPermission)) return false
        val base = baseCommandOf(buffer) ?: return false
        // Completing the command name itself is governed by the command list the client was sent.
        if (buffer.removePrefix("/").trimStart().none { it.isWhitespace() }) return false
        val clean = stripVanillaNamespace(base)
        return isHiddenCommand(sender, base, s) || clean in s.honeypotCommands || base in s.honeypotCommands
    }

    private fun isHiddenCommand(player: Player, label: String, s: Settings): Boolean {
        val clean = stripVanillaNamespace(label)
        if (clean in s.protectedCommands) return true
        if (s.blockCommonPluginCommands && clean in s.commonPluginCommands) return true
        if (s.blockBukkitCommands && label != clean) return true
        if (s.blockNamespacedCommands && ':' in label) return true
        if (s.aggressiveMode) {
            val cmd = server.getPluginCommand(label)
            if (cmd != null && cmd.plugin != this && !player.hasPermission("$label.use")) return true
        }
        return false
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        detector.forgetPlayer(event.player.uniqueId)
    }

    private fun handleProtectedCommand(player: Player, command: String, msg: String, s: Settings) {
        when (command) {
            "plugins", "pl" -> handlePluginsCommand(player, msg, s)
            "version", "ver", "about" -> handleVersionCommand(player, msg, s)
            "help", "?" -> handleSimpleCommand(player, msg, s, "fake-help")
            "icanhasbukkit" -> handleSimpleCommand(player, msg, s, "fake-icanhasbukkit")
            else -> messages.unknownCommand(player, msg)
        }
    }

    private fun handlePluginsCommand(player: Player, msg: String, s: Settings) {
        when (s.hideMode) {
            "empty" -> messages.send(player, "empty-plugin-list")
            "fake-list" -> {
                val plugins = s.fakePlugins.ifEmpty { listOf("ServerCore", "WorldManager") }
                messages.send(player, "fake-plugin-list", "count" to plugins.size, "plugins" to plugins.joinToString(", "))
            }
            "permission-denied" -> messages.permissionDenied(player)
            else -> messages.unknownCommand(player, msg)
        }
    }

    private fun handleVersionCommand(player: Player, msg: String, s: Settings) {
        when (s.hideMode) {
            "fake-list" -> messages.send(player, "fake-version", "brand" to s.fakeServerBrand, "version" to server.minecraftVersion)
            "empty" -> messages.send(player, "version-disabled")
            "permission-denied" -> messages.permissionDenied(player)
            else -> messages.unknownCommand(player, msg)
        }
    }

    private fun handleSimpleCommand(player: Player, msg: String, s: Settings, key: String) {
        when (s.hideMode) {
            "empty", "fake-list" -> messages.send(player, key)
            "permission-denied" -> messages.permissionDenied(player)
            else -> messages.unknownCommand(player, msg)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!command.name.equals("pluginguard", ignoreCase = true)) return false

        if (!sender.hasPermission("pluginguard.reload")) {
            messages.send(sender, "no-permission")
            return true
        }

        when (args.firstOrNull()?.lowercase()) {
            null -> messages.send(sender, "help")
            "reload" -> {
                settings = loadSettings()
                messages.reload()
                messages.send(sender, "reloaded")
            }
            "update" -> io.github.darkstarworks.pluginpulse.PluginPulse.handleUpdateCommand(
                this, sender, args.copyOfRange(1, args.size)
            )
            "status" -> {
                val s = settings
                val on = messages.word(true)
                val off = messages.word(false)
                messages.send(
                    sender, "status",
                    "plugins" to server.pluginManager.plugins.size,
                    "hide-mode" to s.hideMode,
                    "tab" to messages.word(s.hideTabCompletion),
                    "brand" to if (s.hideServerBrand) s.fakeServerBrand else messages.text("status-real-brand"),
                    "channels" to messages.word(s.hidePluginChannels),
                    "aggressive" to messages.word(s.aggressiveMode),
                    "detection" to messages.word(s.detectionEnabled),
                    "honeypots" to s.honeypotCommands.size,
                    "alert-commands" to s.alertCommands.size,
                    "discord" to if (s.discordWebhook.isNotEmpty()) on else off,
                    "file-log" to if (s.logToFile) on else off,
                )
            }
            else -> messages.send(sender, "unknown-subcommand")
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!command.name.equals("pluginguard", ignoreCase = true)) return emptyList()
        if (!sender.hasPermission("pluginguard.reload")) return emptyList()
        if (args.size == 1) {
            return listOf("reload", "status", "update").filter { it.startsWith(args[0].lowercase()) }
        }
        if (args.size == 2 && args[0].equals("update", ignoreCase = true)) {
            return listOf("check", "download", "restore", "ignore", "unignore", "status")
                .filter { it.startsWith(args[1].lowercase()) }
        }
        return emptyList()
    }

    private companion object {
        val HIDE_MODES = listOf("unknown-command", "empty", "fake-list", "permission-denied")

        /** The command name of a chat line, lowercased, without the slash; null if there is none. */
        fun baseCommandOf(line: String): String? {
            var start = if (line.startsWith('/')) 1 else 0
            // "/  plugins" is still routed by some dispatchers, so skip padding after the slash.
            while (start < line.length && line[start].isWhitespace()) start++
            var end = start
            while (end < line.length && !line[end].isWhitespace()) end++
            return if (start >= end) null else line.substring(start, end).lowercase()
        }

        fun stripVanillaNamespace(label: String): String = when {
            label.startsWith("bukkit:") -> label.substring(7)
            label.startsWith("minecraft:") -> label.substring(10)
            else -> label
        }
    }
}
