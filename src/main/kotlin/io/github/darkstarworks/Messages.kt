@file:Suppress("DEPRECATION")

package io.github.darkstarworks

import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader

/** messages.yml, plus the two replies copied from the server itself. */
class Messages(private val plugin: PluginGuard) {

    @Volatile
    private var yaml = YamlConfiguration()

    fun reload() {
        val file = File(plugin.dataFolder, "messages.yml")
        if (!file.exists()) plugin.saveResource("messages.yml", false)
        val loaded = YamlConfiguration.loadConfiguration(file)
        // Keys missing from an older file fall back to the shipped text.
        plugin.getResource("messages.yml")?.use {
            loaded.setDefaults(YamlConfiguration.loadConfiguration(InputStreamReader(it, Charsets.UTF_8)))
        }
        yaml = loaded
    }

    fun send(to: CommandSender, key: String, vararg vars: Pair<String, Any>) {
        val lines = if (yaml.isList(key)) yaml.getStringList(key) else listOfNotNull(yaml.getString(key))
        for (line in lines) {
            // Colours are applied before the values go in, so a typed "&" stays a plain "&".
            var text = ChatColor.translateAlternateColorCodes('&', line)
            for ((name, value) in vars) text = text.replace("{$name}", value.toString().replace("§", ""))
            if (Platform.isPaper) PaperChat.sendLegacy(to, text) else to.sendMessage(text)
        }
    }

    fun word(on: Boolean): String = yaml.getString(if (on) "status-on" else "status-off")!!

    fun text(key: String): String = yaml.getString(key) ?: key

    /** The exact reply the server gives to a command that does not exist. */
    fun unknownCommand(to: CommandSender, commandLine: String) {
        val spigotText = spigotUnknownCommand()
        if (Platform.isPaper) {
            if (spigotText?.isEmpty() != true) PaperChat.unknownCommand(to, commandLine)
        } else {
            val msg = spigotText ?: "Unknown command. Type \"/help\" for help."
            if (msg.isNotEmpty()) to.sendMessage(ChatColor.translateAlternateColorCodes('&', msg))
        }
    }

    /** The exact reply the server gives when a command is denied. */
    fun permissionDenied(to: CommandSender) {
        if (Platform.isPaper) {
            PaperChat.permissionDenied(to)
        } else {
            to.sendMessage(
                ChatColor.RED.toString() + "I'm sorry, but you do not have permission to perform this command. " +
                    "Please contact the server administrators if you believe that this is in error."
            )
        }
    }

    // On Paper this text only decides whether anything is sent; an empty value means silence.
    private fun spigotUnknownCommand(): String? = try {
        plugin.server.spigot().spigotConfig.getString("messages.unknown-command")
    } catch (_: Throwable) {
        null
    }
}
