package io.github.darkstarworks

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

/** Adventure output, kept in its own object so Spigot never loads it. */
internal object PaperChat {

    fun sendLegacy(to: CommandSender, text: String) {
        to.sendMessage(LegacyComponentSerializer.legacySection().deserialize(text))
    }

    // Built piece for piece like Paper's own parser error. The parser stops at the first
    // character, so the whole line is the underlined part.
    fun unknownCommand(to: CommandSender, commandLine: String) {
        val input = commandLine.removePrefix("/")
        var context = Component.empty()
            .color(NamedTextColor.GRAY)
            .clickEvent(ClickEvent.suggestCommand("/$input"))
        if (input.isNotEmpty()) {
            context = context.append(Component.text(input, NamedTextColor.RED, TextDecoration.UNDERLINED))
        }
        context = context.append(Component.translatable("command.context.here", NamedTextColor.RED, TextDecoration.ITALIC))
        to.sendMessage(
            Component.text().color(NamedTextColor.RED)
                .append(Component.translatable("command.unknown.command"))
                .append(Component.newline())
                .append(context)
                .build()
        )
    }

    fun permissionDenied(to: CommandSender) {
        to.sendMessage(Bukkit.permissionMessage())
    }
}
