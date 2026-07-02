package io.github.darkstarworks

import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPromise
import org.bukkit.Bukkit

/**
 * Spoofs the in-game server brand. [PingListener] only rewrites the brand shown on the
 * server-list ping; the brand a client sees in the F3 debug screen (and that "server brand"
 * client mods read) travels separately, in the `minecraft:brand` plugin-message payload the
 * server pushes down every connection. On Paper 1.20.2+ that payload is sent during the
 * CONFIGURATION phase — before PlayerJoinEvent fires — so a per-player inject at join would
 * miss it. We therefore inject at the server-channel level, the way TinyProtocol does, so our
 * handler is present in every child connection's pipeline before login completes.
 *
 * Reflection on plain paper-api (no paperweight, no ProtocolLib), matching the project's
 * ReflectiveTagStripper approach: server-internal types are resolved at runtime and the whole
 * thing fails OPEN. Any reflection hiccup (e.g. Spigot's remapped internals) logs one warning
 * and leaves the brand untouched — the rest of the plugin is unaffected.
 */
class BrandSpoofer(private val plugin: PluginGuard) {

    private val handlerName = "pluginguard_brand"

    // The ClientboundCustomPayloadPacket / BrandPayload record types are matched by simple name
    // at packet-write time, so they need no Class.forName here and stay mapping-agnostic.

    @Volatile
    private var installed = false

    // Server channels we added [serverChannelHandler] to, so we can pull it back out on disable.
    private val hookedServerChannels = ArrayList<Channel>()

    // ChannelInitializer is @Sharable by contract, so one instance can seed every child channel.
    private val childInitializer = object : ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel) {
            // The child's own pipeline ("packet_handler" et al.) isn't wired yet when initChannel
            // runs; defer onto the channel's event loop so packet_handler exists to anchor before.
            ch.eventLoop().execute {
                try {
                    val pipeline = ch.pipeline()
                    if (pipeline.get("packet_handler") != null && pipeline.get(handlerName) == null) {
                        pipeline.addBefore("packet_handler", handlerName, BrandHandler())
                    }
                } catch (_: Throwable) {
                    // Channel closed mid-handshake, or an unexpected pipeline shape — skip it.
                }
            }
        }
    }

    @ChannelHandler.Sharable
    private inner class ServerChannelHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            // The acceptor passes each freshly accepted child Channel through as an inbound message.
            (msg as? Channel)?.pipeline()?.addFirst(childInitializer)
            ctx.fireChannelRead(msg)
        }
    }

    private val serverChannelHandler = ServerChannelHandler()

    fun enable() {
        // The listening socket may not be bound yet when a STARTUP-load plugin enables. Retry a
        // few times on the global-region scheduler until the server's channel list is populated.
        tryInstall(attemptsLeft = 20)
    }

    private fun tryInstall(attemptsLeft: Int) {
        val channels = try {
            serverChannels()
        } catch (e: Throwable) {
            plugin.logger.warning("In-game brand spoofing unavailable (server internals not reachable): ${e.message}")
            return
        }

        if (channels.isEmpty()) {
            if (attemptsLeft <= 0) {
                plugin.logger.warning("In-game brand spoofing: no bound server channel found; giving up")
                return
            }
            plugin.server.globalRegionScheduler.runDelayed(plugin, { tryInstall(attemptsLeft - 1) }, 5L)
            return
        }

        for (future in channels) {
            val serverChannel = future.channel() ?: continue
            serverChannel.eventLoop().execute {
                if (serverChannel.pipeline().get("PluginGuardServerChannelHandler") == null) {
                    serverChannel.pipeline().addFirst("PluginGuardServerChannelHandler", serverChannelHandler)
                    synchronized(hookedServerChannels) { hookedServerChannels.add(serverChannel) }
                }
            }
        }
        installed = true
        plugin.logger.info("In-game brand spoofing active on ${channels.size} listener(s)")
    }

    fun disable() {
        if (!installed) return
        val snapshot = synchronized(hookedServerChannels) { ArrayList(hookedServerChannels).also { hookedServerChannels.clear() } }
        for (serverChannel in snapshot) {
            try {
                serverChannel.eventLoop().execute {
                    if (serverChannel.pipeline().get("PluginGuardServerChannelHandler") != null) {
                        serverChannel.pipeline().remove("PluginGuardServerChannelHandler")
                    }
                }
            } catch (_: Throwable) {
                // Shutting down — Netty tears the pipeline down with the channel anyway.
            }
        }
        installed = false
    }

    /** MinecraftServer.getConnection() -> the List<ChannelFuture> of bound listeners. */
    @Suppress("UNCHECKED_CAST")
    private fun serverChannels(): List<ChannelFuture> {
        val craftServer = Bukkit.getServer()
        val mcServer = craftServer.javaClass.getMethod("getServer").invoke(craftServer)

        // getConnection() returns the ServerConnectionListener; find it by return-type name so we
        // don't depend on the (mapping-specific) method name beyond the common Mojang one.
        val connection = mcServer.javaClass.methods.firstOrNull { m ->
            m.parameterCount == 0 && m.returnType.simpleName == "ServerConnectionListener"
        }?.invoke(mcServer)
            ?: mcServer.javaClass.getMethod("getConnection").invoke(mcServer)

        // The ServerConnectionListener has two List fields: channels (List<ChannelFuture>) and
        // connections (List<Connection>). Pick the one whose elements are ChannelFutures.
        for (field in connection.javaClass.declaredFields) {
            if (!List::class.java.isAssignableFrom(field.type)) continue
            field.isAccessible = true
            val list = field.get(connection) as? List<*> ?: continue
            if (list.isEmpty()) continue
            if (list.first() is ChannelFuture) return list as List<ChannelFuture>
        }
        return emptyList()
    }

    private inner class BrandHandler : ChannelDuplexHandler() {
        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            val out = try {
                rewriteBrand(msg) ?: msg
            } catch (_: Throwable) {
                msg // fail open — never drop a packet over a spoofing hiccup
            }
            super.write(ctx, out, promise)
        }
    }

    /**
     * If [msg] is a ClientboundCustomPayloadPacket carrying a BrandPayload, return a copy whose
     * brand is the configured fake brand; otherwise null (caller forwards the original untouched).
     */
    private fun rewriteBrand(msg: Any): Any? {
        if (!plugin.shouldHideServerBrand()) return null
        val cls = msg.javaClass
        if (cls.simpleName != "ClientboundCustomPayloadPacket" || !cls.isRecord) return null

        val components = cls.recordComponents
        for (comp in components) {
            val payload = comp.accessor.invoke(msg) ?: continue
            if (payload.javaClass.simpleName != "BrandPayload") continue

            val newPayload = rebuildBrandPayload(payload) ?: return null
            val args = components.map { if (it === comp) newPayload else it.accessor.invoke(msg) }
            val ctor = cls.getDeclaredConstructor(*components.map { it.type }.toTypedArray())
            ctor.isAccessible = true
            return ctor.newInstance(*args.toTypedArray())
        }
        return null
    }

    /** BrandPayload is a record of a single String. Rebuild it with the spoofed brand. */
    private fun rebuildBrandPayload(payload: Any): Any? {
        val ctor = payload.javaClass.getDeclaredConstructor(String::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(plugin.fakeBrand())
    }
}
