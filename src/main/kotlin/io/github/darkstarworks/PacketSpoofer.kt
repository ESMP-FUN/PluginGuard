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
import java.util.concurrent.ConcurrentHashMap

/**
 * Rewrites the `minecraft:brand` and `minecraft:register` payloads before they reach the client.
 *
 * Both go out in the configuration phase, before PlayerJoinEvent, so the handler is added to every
 * connection from the server channel. Server types are found by reflection; any failure sends the
 * packet unchanged.
 */
class PacketSpoofer(private val plugin: PluginGuard) {

    private val handlerName = "pluginguard_brand"
    private val serverHandlerName = "PluginGuardServerChannelHandler"

    @Volatile
    private var installed = false

    private val hookedServerChannels = ArrayList<Channel>()

    // Connections carrying our handler. A handler left behind after this plugin is disabled (or
    // replaced by an update installed without a restart) would keep the old class loader alive.
    private val hookedChildren: MutableSet<Channel> = ConcurrentHashMap.newKeySet()

    private val childInitializer = object : ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel) {
            // packet_handler isn't in the child pipeline yet when initChannel runs.
            ch.eventLoop().execute {
                try {
                    val pipeline = ch.pipeline()
                    if (installed && pipeline.get("packet_handler") != null && pipeline.get(handlerName) == null) {
                        pipeline.addBefore("packet_handler", handlerName, PayloadHandler())
                        hookedChildren.add(ch)
                        ch.closeFuture().addListener { hookedChildren.remove(ch) }
                    }
                } catch (_: Throwable) {
                    // Closed mid-handshake.
                }
            }
        }
    }

    @ChannelHandler.Sharable
    private inner class ServerChannelHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            (msg as? Channel)?.pipeline()?.addFirst(childInitializer)
            ctx.fireChannelRead(msg)
        }
    }

    private val serverChannelHandler = ServerChannelHandler()

    fun enable() {
        // A STARTUP plugin enables before the listener socket is bound; retry until it is.
        tryInstall(attemptsLeft = 20)
    }

    private fun tryInstall(attemptsLeft: Int) {
        val channels = try {
            serverChannels()
        } catch (e: Throwable) {
            plugin.logger.warning("Can't hide the in-game server brand or plugin channels on this server version (${e.message}).")
            return
        }

        if (channels.isEmpty()) {
            if (attemptsLeft <= 0) {
                plugin.logger.warning("Can't hide the in-game server brand or plugin channels: the server never opened its port.")
                return
            }
            Platform.runGlobalLater(plugin, 5L) { tryInstall(attemptsLeft - 1) }
            return
        }

        installed = true
        for (future in channels) {
            val serverChannel = future.channel() ?: continue
            serverChannel.eventLoop().execute {
                if (serverChannel.pipeline().get(serverHandlerName) == null) {
                    serverChannel.pipeline().addFirst(serverHandlerName, serverChannelHandler)
                    synchronized(hookedServerChannels) { hookedServerChannels.add(serverChannel) }
                }
            }
        }
    }

    fun disable() {
        if (!installed) return
        installed = false
        val servers = synchronized(hookedServerChannels) { ArrayList(hookedServerChannels).also { hookedServerChannels.clear() } }
        for (ch in servers) removeHandler(ch, serverHandlerName)
        for (ch in hookedChildren) removeHandler(ch, handlerName)
        hookedChildren.clear()
    }

    private fun removeHandler(ch: Channel, name: String) {
        try {
            ch.eventLoop().execute {
                if (ch.pipeline().get(name) != null) ch.pipeline().remove(name)
            }
        } catch (_: Throwable) {
        }
    }

    /** MinecraftServer.getConnection() -> the List<ChannelFuture> of bound listeners. */
    @Suppress("UNCHECKED_CAST")
    private fun serverChannels(): List<ChannelFuture> {
        val craftServer = Bukkit.getServer()
        val mcServer = craftServer.javaClass.getMethod("getServer").invoke(craftServer)

        val connection = mcServer.javaClass.methods.firstOrNull { m ->
            m.parameterCount == 0 && m.returnType.simpleName == "ServerConnectionListener"
        }?.invoke(mcServer)
            ?: mcServer.javaClass.getMethod("getConnection").invoke(mcServer)

        for (field in connection.javaClass.declaredFields) {
            if (!List::class.java.isAssignableFrom(field.type)) continue
            field.isAccessible = true
            val list = field.get(connection) as? List<*> ?: continue
            if (list.isEmpty()) continue
            if (list.first() is ChannelFuture) return list as List<ChannelFuture>
        }
        return emptyList()
    }

    private inner class PayloadHandler : ChannelDuplexHandler() {
        override fun write(ctx: ChannelHandlerContext, msg: Any, promise: ChannelPromise) {
            val out = try {
                rewrite(msg) ?: msg
            } catch (_: Throwable) {
                msg
            }
            if (out === DROP) {
                promise.setSuccess()
                return
            }
            super.write(ctx, out, promise)
        }
    }

    /** A replacement packet, [DROP] to send nothing, or null to send [msg] unchanged. */
    private fun rewrite(msg: Any): Any? {
        val cls = msg.javaClass
        if (cls.simpleName != "ClientboundCustomPayloadPacket" || !cls.isRecord) return null

        val components = cls.recordComponents
        for (comp in components) {
            val payload = comp.accessor.invoke(msg) ?: continue
            val newPayload = when (payload.javaClass.simpleName) {
                "BrandPayload" -> if (plugin.currentSettings().hideServerBrand) rebuildBrand(payload) else null
                "DiscardedPayload" -> filterChannels(payload)
                else -> null
            } ?: return null
            if (newPayload === DROP) return DROP

            val args = components.map { if (it === comp) newPayload else it.accessor.invoke(msg) }
            val ctor = cls.getDeclaredConstructor(*components.map { it.type }.toTypedArray())
            ctor.isAccessible = true
            return ctor.newInstance(*args.toTypedArray())
        }
        return null
    }

    private fun rebuildBrand(payload: Any): Any {
        val ctor = payload.javaClass.getDeclaredConstructor(String::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(plugin.currentSettings().fakeServerBrand)
    }

    /** Strips hidden channels from a register/unregister payload: channel names separated by NUL. */
    private fun filterChannels(payload: Any): Any? {
        val s = plugin.currentSettings()
        if (!s.hidePluginChannels) return null
        val cls = payload.javaClass
        if (!cls.isRecord) return null
        val components = cls.recordComponents
        val values = components.map { it.accessor.invoke(payload) }
        val id = values.firstOrNull { it != null && it !is ByteArray }?.toString() ?: return null
        if (id != "minecraft:register" && id != "minecraft:unregister") return null
        val dataIndex = values.indexOfFirst { it is ByteArray }
        if (dataIndex < 0) return null

        val channels = String(values[dataIndex] as ByteArray, Charsets.UTF_8).split('\u0000').filter { it.isNotEmpty() }
        val kept = channels.filter { it.lowercase() in s.allowedPluginChannels }
        if (kept.size == channels.size) return null
        if (kept.isEmpty()) return DROP

        val args = values.toMutableList()
        args[dataIndex] = kept.joinToString("") { it + "\u0000" }.toByteArray(Charsets.UTF_8)
        val ctor = cls.getDeclaredConstructor(*components.map { it.type }.toTypedArray())
        ctor.isAccessible = true
        return ctor.newInstance(*args.toTypedArray())
    }

    private companion object {
        val DROP = Any()
    }
}
