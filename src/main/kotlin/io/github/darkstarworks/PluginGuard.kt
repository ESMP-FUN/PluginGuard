package io.github.darkstarworks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerCommandSendEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin

class PluginGuard : JavaPlugin(), Listener {

    // Immutable snapshot of every config-derived value. Swapped atomically on reload via
    // a @Volatile reference so concurrent event handlers on different Folia region threads
    // always observe a consistent set of values without taking any lock.
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
        // Logging / detection
        val loggingEnabled: Boolean,
        val logToFile: Boolean,
        val logIndividualProbes: Boolean,
        val detectionEnabled: Boolean,
        val detectionScoreThreshold: Int,
        val detectionWindowSeconds: Int,
        val detectionAlertCooldownSeconds: Int,
        val notifyPermission: String,
    )

    @Volatile
    private lateinit var settings: Settings

    private val detector = ProbeDetector(this)
    private val brandSpoofer = BrandSpoofer(this)

    fun currentSettings(): Settings = settings

    override fun onEnable() {
        saveDefaultConfig()
        settings = loadSettings()
        server.pluginManager.registerEvents(this, this)
        registerPaperListeners()
        // In-game brand spoofing (F3 / client "server brand" mods) — the ping-only PingListener
        // can't reach it. Injects at the Netty layer; the handler consults hideServerBrand live at
        // write time, so we install once here and a later /pluginguard reload can toggle it freely.
        // Fails open on servers that don't expose these internals (e.g. Spigot's remapped classes).
        brandSpoofer.enable()
        logger.info("PluginGuard enabled - protecting ${server.pluginManager.plugins.size} plugins")
    }

    override fun onDisable() {
        detector.forgetAll()
        brandSpoofer.disable()
    }

    private fun registerPaperListeners() {
        // PaperServerListPingEvent only exists on Paper and its forks. Probe via reflection so
        // the plugin still loads on Spigot/Bukkit (server brand spoofing simply won't apply).
        try {
            Class.forName("com.destroystokyo.paper.event.server.PaperServerListPingEvent")
            server.pluginManager.registerEvents(PingListener(this), this)
        } catch (_: ClassNotFoundException) {
            logger.warning("PaperServerListPingEvent unavailable - server-brand spoofing disabled (Paper or a Paper fork required)")
        }
    }

    fun shouldHideServerBrand(): Boolean = settings.hideServerBrand
    fun fakeBrand(): String = settings.fakeServerBrand

    private fun loadSettings(): Settings {
        reloadConfig()
        return Settings(
            hideMode = config.getString("hide-mode", "unknown-command")!!,
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
            loggingEnabled = config.getBoolean("logging.enabled", true).let {
                // logging section is implicitly enabled if any sub-toggle is on
                it || config.getBoolean("logging.log-to-file", false) ||
                    config.getBoolean("logging.log-individual-probes", false) ||
                    config.getBoolean("logging.detection.enabled", true)
            },
            logToFile = config.getBoolean("logging.log-to-file", false),
            logIndividualProbes = config.getBoolean("logging.log-individual-probes", false),
            detectionEnabled = config.getBoolean("logging.detection.enabled", true),
            detectionScoreThreshold = config.getInt("logging.detection.score-threshold", 5),
            detectionWindowSeconds = config.getInt("logging.detection.window-seconds", 60),
            detectionAlertCooldownSeconds = config.getInt("logging.detection.alert-cooldown-seconds", 300),
            notifyPermission = config.getString("logging.detection.notify-permission", "pluginguard.alerts")!!,
        )
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommandPreprocess(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val s = settings
        if (player.hasPermission(s.bypassPermission)) return

        // Extract just the base command without allocating a lowercased copy of the whole message
        // or splitting on spaces. Hot path: runs on every command a player issues.
        val msg = event.message
        var start = if (msg.startsWith('/')) 1 else 0
        // Skip any whitespace between the slash and the command. A prober can pad "/  plugins" or
        // "/\tplugins"; some dispatchers tolerate that and route it while a naive slice would miss
        // the token entirely. Mirror the dispatcher: find the first non-whitespace char.
        while (start < msg.length && msg[start].isWhitespace()) start++
        var end = start
        while (end < msg.length && !msg[end].isWhitespace()) end++
        if (start >= end) return
        val baseCommand = msg.substring(start, end).lowercase()

        val cleanCommand = when {
            baseCommand.startsWith("bukkit:") -> baseCommand.substring(7)
            baseCommand.startsWith("minecraft:") -> baseCommand.substring(10)
            else -> baseCommand
        }

        // Honeypot first — by definition the highest-signal probe and we never want to forward it.
        if (cleanCommand in s.honeypotCommands || baseCommand in s.honeypotCommands) {
            event.isCancelled = true
            sendUnknownCommand(player)
            detector.record(player, ProbeDetector.Category.HONEYPOT, "honeypot:$baseCommand")
            return
        }

        when {
            baseCommand in s.protectedCommands || cleanCommand in s.protectedCommands -> {
                event.isCancelled = true
                if (baseCommand.startsWith("bukkit:") && s.redirectBukkitCommands) {
                    handlePluginsCommand(player, s)
                } else {
                    handleProtectedCommand(player, cleanCommand, s)
                }
                // Skip /help and /? — far too commonly typed legitimately to be useful signal.
                if (cleanCommand != "help" && cleanCommand != "?") {
                    val cat = if (baseCommand.startsWith("bukkit:") || baseCommand.startsWith("minecraft:") || cleanCommand == "icanhasbukkit")
                        ProbeDetector.Category.HIGH
                    else
                        ProbeDetector.Category.MEDIUM
                    detector.record(player, cat, "/$baseCommand")
                }
            }
            (baseCommand.startsWith("bukkit:") || baseCommand.startsWith("minecraft:")) && s.blockBukkitCommands -> {
                event.isCancelled = true
                sendUnknownCommand(player)
                detector.record(player, ProbeDetector.Category.HIGH, baseCommand)
            }
            // A namespaced command like /essentials:home confirms the plugin exists even when the
            // bare alias is blocked — the namespace IS the plugin name. Block every namespace.
            s.blockNamespacedCommands && ':' in baseCommand -> {
                event.isCancelled = true
                sendUnknownCommand(player)
                detector.record(player, ProbeDetector.Category.HIGH, baseCommand)
            }
            baseCommand in s.commonPluginCommands && s.blockCommonPluginCommands -> {
                event.isCancelled = true
                sendUnknownCommand(player)
                detector.record(player, ProbeDetector.Category.LOW, "/$baseCommand")
            }
            s.aggressiveMode && !player.hasPermission("$baseCommand.use") -> {
                if (server.getPluginCommand(baseCommand) != null) {
                    event.isCancelled = true
                    sendUnknownCommand(player)
                }
            }
            // A plugin's own "You don't have permission" reply confirms the plugin exists.
            // If the player can't run the command anyway, answer with the vanilla unknown-command
            // line before the plugin gets a chance to leak itself. Not tracked by the detector:
            // legitimate players hit permission walls all the time.
            s.blockUnknownCommands -> {
                val cmd = server.getPluginCommand(baseCommand)
                if (cmd != null && cmd.plugin != this && !cmd.testPermissionSilent(player)) {
                    event.isCancelled = true
                    sendUnknownCommand(player)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onCommandSend(event: PlayerCommandSendEvent) {
        val s = settings
        if (!s.hideTabCompletion) return
        if (event.player.hasPermission(s.bypassPermission)) return

        event.commands.removeIf { command ->
            val cleanCommand = when {
                command.startsWith("bukkit:") -> command.substring(7).lowercase()
                command.startsWith("minecraft:") -> command.substring(10).lowercase()
                else -> command.lowercase()
            }

            cleanCommand in s.protectedCommands ||
                    (cleanCommand in s.commonPluginCommands && s.blockCommonPluginCommands) ||
                    (s.blockBukkitCommands && (command.startsWith("bukkit:") || command.startsWith("minecraft:"))) ||
                    // Namespaced completions (essentials:home, luckperms:lp, ...) spell out the
                    // plugin list by themselves — strip every namespaced entry from suggestions.
                    (s.blockNamespacedCommands && ':' in command)
        }

        if (s.aggressiveMode) {
            event.commands.removeIf { command ->
                val cmd = server.getPluginCommand(command)
                cmd != null && cmd.plugin != this && !event.player.hasPermission("$command.use")
            }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        detector.forgetPlayer(event.player.uniqueId)
    }

    private fun handleProtectedCommand(player: Player, command: String, s: Settings) {
        when (command) {
            "plugins", "pl" -> handlePluginsCommand(player, s)
            "version", "ver", "about" -> handleVersionCommand(player, s)
            "help", "?" -> handleHelpCommand(player, s)
            "icanhasbukkit" -> handleICanHasBukkitCommand(player, s)
            else -> sendUnknownCommand(player)
        }
    }

    private fun handlePluginsCommand(player: Player, s: Settings) {
        when (s.hideMode) {
            "unknown-command" -> sendUnknownCommand(player)
            "empty" -> player.sendMessage(Component.text("Plugins (0):", NamedTextColor.WHITE))
            "fake-list" -> {
                val plugins = s.fakePlugins.ifEmpty { listOf("ServerCore", "WorldManager") }
                player.sendMessage(
                    Component.text("Plugins (${plugins.size}): ", NamedTextColor.WHITE)
                        .append(Component.text(plugins.joinToString(", "), NamedTextColor.GREEN))
                )
            }
            "permission-denied" -> player.sendMessage(
                Component.text("I'm sorry, but you do not have permission to perform this command.", NamedTextColor.RED)
            )
        }
    }

    private fun handleVersionCommand(player: Player, s: Settings) {
        when (s.hideMode) {
            "unknown-command" -> sendUnknownCommand(player)
            "fake-list" -> player.sendMessage(
                Component.text("This server is running ", NamedTextColor.WHITE)
                    .append(Component.text("Paper", NamedTextColor.GREEN))
                    .append(Component.text(" version ", NamedTextColor.WHITE))
                    .append(Component.text("${s.fakeServerBrand} (MC: ${server.minecraftVersion})", NamedTextColor.GREEN))
            )
            else -> player.sendMessage(Component.text("This command has been disabled.", NamedTextColor.RED))
        }
    }

    private fun handleHelpCommand(player: Player, s: Settings) {
        if (s.hideMode == "unknown-command") {
            sendUnknownCommand(player)
        } else {
            player.sendMessage(Component.text("--------- Help: Index ---------", NamedTextColor.GOLD))
            player.sendMessage(Component.text("Use /help [n] to get page n of help.", NamedTextColor.GRAY))
        }
    }

    private fun handleICanHasBukkitCommand(player: Player, s: Settings) {
        when (s.hideMode) {
            "unknown-command" -> sendUnknownCommand(player)
            else -> player.sendMessage(Component.text("This server is not running Bukkit!", NamedTextColor.WHITE))
        }
    }

    private fun sendUnknownCommand(player: Player) {
        player.sendMessage(Component.text("Unknown command. Type \"/help\" for help.", NamedTextColor.RED))
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (!command.name.equals("pluginguard", ignoreCase = true)) return false

        if (!sender.hasPermission("pluginguard.reload")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(Component.text("PluginGuard Commands:", NamedTextColor.GOLD))
            sender.sendMessage(
                Component.text("/pluginguard reload ", NamedTextColor.YELLOW)
                    .append(Component.text("- Reload configuration", NamedTextColor.GRAY))
            )
            sender.sendMessage(
                Component.text("/pluginguard status ", NamedTextColor.YELLOW)
                    .append(Component.text("- Show protection status", NamedTextColor.GRAY))
            )
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> {
                settings = loadSettings()
                sender.sendMessage(Component.text("PluginGuard configuration reloaded!", NamedTextColor.GREEN))
            }
            "status" -> {
                val s = settings
                sender.sendMessage(Component.text("PluginGuard Status:", NamedTextColor.GOLD))
                fun row(label: String, value: String) = sender.sendMessage(
                    Component.text("$label: ", NamedTextColor.GRAY)
                        .append(Component.text(value, NamedTextColor.WHITE))
                )
                row("Protected Plugins", "${server.pluginManager.plugins.size}")
                row("Hide Mode", s.hideMode)
                row("Tab Completion", if (s.hideTabCompletion) "Hidden" else "Visible")
                row("Server Brand", if (s.hideServerBrand) s.fakeServerBrand else "Real")
                row("Aggressive Mode", if (s.aggressiveMode) "Enabled" else "Disabled")
                row("Detection", if (s.detectionEnabled) "On (threshold ${s.detectionScoreThreshold} / ${s.detectionWindowSeconds}s)" else "Off")
                row("Honeypots", "${s.honeypotCommands.size} configured")
                row("File Log", if (s.logToFile) "On" else "Off")
            }
            else -> sender.sendMessage(Component.text("Unknown subcommand. Use /pluginguard for help.", NamedTextColor.RED))
        }
        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<String>): List<String> {
        if (!command.name.equals("pluginguard", ignoreCase = true)) return emptyList()
        if (!sender.hasPermission("pluginguard.reload")) return emptyList()
        if (args.size == 1) {
            return listOf("reload", "status").filter { it.startsWith(args[0].lowercase()) }
        }
        return emptyList()
    }
}
