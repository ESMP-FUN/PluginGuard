package io.github.darkstarworks

import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent
import com.destroystokyo.paper.event.server.GS4QueryEvent
import com.destroystokyo.paper.event.server.PaperServerListPingEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

/** Paper-only events, kept apart so Spigot never resolves these classes. */
class PaperListener(private val plugin: PluginGuard) : Listener {

    @EventHandler(priority = EventPriority.LOW)
    fun onServerListPing(event: PaperServerListPingEvent) {
        val s = plugin.currentSettings()
        if (!s.hideServerBrand) return
        event.protocolVersion = event.client.protocolVersion
        event.version = s.fakeServerBrand
    }

    // The query port (enable-query in server.properties) answers with the full plugin list and
    // server software to anyone who asks, no login needed.
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuery(event: GS4QueryEvent) {
        val s = plugin.currentSettings()
        val builder = event.response.toBuilder().clearPlugins()
        if (s.hideServerBrand) builder.serverVersion(s.fakeServerBrand)
        event.response = builder.build()
    }

    // See PluginGuard.onTabComplete.
    @EventHandler(priority = EventPriority.LOWEST)
    fun onAsyncTabComplete(event: AsyncTabCompleteEvent) {
        if (event.isCommand && plugin.hidesSuggestionsFor(event.sender, event.buffer)) {
            event.completions = mutableListOf()
            event.isHandled = true
            event.isCancelled = true
        }
    }
}
