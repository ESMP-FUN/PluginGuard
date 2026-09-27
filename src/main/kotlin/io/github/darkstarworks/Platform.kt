package io.github.darkstarworks

import org.bukkit.plugin.Plugin

/** Scheduling that works on Paper, Folia and plain Spigot. */
object Platform {

    /** True on Paper and its forks: region schedulers and Adventure are available. */
    val isPaper: Boolean = try {
        Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
        Class.forName("net.kyori.adventure.audience.Audience")
        true
    } catch (_: ClassNotFoundException) {
        false
    }

    fun runGlobal(plugin: Plugin, task: Runnable) {
        if (isPaper) plugin.server.globalRegionScheduler.execute(plugin, task)
        else plugin.server.scheduler.runTask(plugin, task)
    }

    fun runGlobalLater(plugin: Plugin, delayTicks: Long, task: Runnable) {
        if (isPaper) plugin.server.globalRegionScheduler.runDelayed(plugin, { task.run() }, delayTicks)
        else plugin.server.scheduler.runTaskLater(plugin, task, delayTicks)
    }

    fun runAsync(plugin: Plugin, task: Runnable) {
        if (isPaper) plugin.server.asyncScheduler.runNow(plugin) { task.run() }
        else plugin.server.scheduler.runTaskAsynchronously(plugin, task)
    }
}
