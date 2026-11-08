package app.katari.desktop.mpris

import app.katari.desktop.dbus.DbusConnection
import app.katari.desktop.dbus.DbusException
import app.katari.desktop.dbus.DbusMessage
import app.katari.desktop.mpris.MprisProperties.PLAYER_INTERFACE
import app.katari.desktop.mpris.MprisProperties.POSITION
import app.katari.desktop.mpris.MprisProperties.ROOT_INTERFACE
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Linux: a media player on the session bus as MPRIS describes one, which shells show in their media widget and send
 * the media keys to. It owns [busName] on a connection of its own, served on a thread of its own, from creation until
 * [close]; closing the connection lets go of the name, which takes the player out of the shells' controls. Shells read
 * the position when they want it, so only its jumps are signalled (`Seeked`): those the state reports away from where
 * playing on from the last one would have got to.
 */
internal class MprisPlayer(
    private val busName: String,
    private val identity: String,
    private val desktopEntry: String,
    private val commands: MprisCommands,
) : AutoCloseable {
    private val latest = AtomicReference<Timed?>()

    @Volatile
    private var closed = false

    @Volatile
    private var bus: DbusConnection? = null

    init {
        Thread(::serve, "MPRIS player").apply { isDaemon = true }.start()
    }

    /** Cheap: the serving thread picks the latest state up. */
    fun update(state: MprisPlayerState) {
        latest.set(Timed(state, TimeSource.Monotonic.markNow()))
        bus?.wakeUp()
    }

    override fun close() {
        closed = true
        bus?.wakeUp()
    }

    private fun serve() {
        val bus = DbusConnection.session() ?: return logcat(LogPriority.WARN) { "No session bus for media controls" }
        try {
            val flags = DbusConnection.NAME_FLAG_ALLOW_REPLACEMENT or
                DbusConnection.NAME_FLAG_REPLACE_EXISTING or
                DbusConnection.NAME_FLAG_DO_NOT_QUEUE
            if (!bus.requestName(busName, flags)) {
                return logcat(LogPriority.WARN) { "Another program holds $busName; no media controls" }
            }
            this.bus = bus
            var shown: Timed? = null
            while (!closed) {
                latest.getAndSet(null)?.let { next ->
                    announce(bus, shown, next)
                    shown = next
                }
                val message = bus.nextMessage(IDLE_WAIT) ?: continue
                if (message.type == DbusMessage.METHOD_CALL) answer(bus, message, shown)
            }
        } catch (e: IOException) {
            logcat(LogPriority.WARN, e) { "The media controls lost the session bus" }
        } catch (e: DbusException) {
            logcat(LogPriority.WARN) { "The session bus refused the media controls: ${e.message}" }
        } finally {
            this.bus = null
            bus.close()
        }
    }

    private fun announce(bus: DbusConnection, shown: Timed?, next: Timed) {
        val before = shown?.let { MprisProperties.player(it.state, 0L) - POSITION }
        val changed = (MprisProperties.player(next.state, 0L) - POSITION).filter { (name, value) ->
            before?.get(name) != value
        }
        if (changed.isNotEmpty()) {
            bus.emitSignal(
                PATH,
                PROPERTIES_INTERFACE,
                "PropertiesChanged",
                "sa{sv}as",
                PLAYER_INTERFACE,
                changed,
                emptyList<String>(),
            )
        }
        if (shown != null && shown.state.trackId == next.state.trackId) {
            val expected = shown.positionAt(next.at)
            if (abs(next.state.positionUs - expected) > SEEK_JUMP_US) {
                bus.emitSignal(PATH, PLAYER_INTERFACE, "Seeked", "x", next.state.positionUs)
            }
        }
    }

    private fun answer(bus: DbusConnection, call: DbusMessage, shown: Timed?) {
        val member = call.member.orEmpty()
        val iface = call.iface ?: INTERFACE_OF_MEMBER[member]
        if (iface == PEER_INTERFACE) return answerPeer(bus, call, member)
        if (call.path != PATH) return bus.replyError(call, UNKNOWN_OBJECT, "No object at ${call.path}")
        when (iface) {
            INTROSPECTABLE_INTERFACE -> bus.reply(call, "s", MPRIS_INTROSPECTION)
            PROPERTIES_INTERFACE -> answerProperties(bus, call, member, shown)
            ROOT_INTERFACE -> when (member) {
                "Raise" -> commands.raise().also { bus.reply(call) }
                // CanQuit is false: the player only ends from the app
                "Quit" -> bus.reply(call)
                else -> bus.replyError(call, UNKNOWN_METHOD, "No method $member")
            }
            PLAYER_INTERFACE -> answerPlayer(bus, call, member, shown)
            else -> bus.replyError(call, UNKNOWN_INTERFACE, "No interface $iface")
        }
    }

    private fun answerPlayer(bus: DbusConnection, call: DbusMessage, member: String, shown: Timed?) {
        val state = shown?.state
        when (member) {
            // The specification has these do nothing when the player can't, rather than fail
            "Next" -> if (state?.canGoNext == true) commands.next()
            "Previous" -> if (state?.canGoPrevious == true) commands.previous()
            "Play" -> if (state != null) commands.play()
            "Pause" -> if (state != null) commands.pause()
            "PlayPause" -> if (state != null) commands.playPause()
            "Stop" -> if (state != null) commands.stop()
            "Seek" -> {
                val length = state?.lengthUs
                val offset = call.body.firstOrNull() as? Long
                // Past the end, the episode ends there, rather than skip to the next as the specification suggests
                if (shown != null && length != null && offset != null) {
                    val position = shown.positionAt(TimeSource.Monotonic.markNow()) + offset
                    commands.setPosition(position.coerceIn(0, length))
                }
            }
            "SetPosition" -> {
                val length = state?.lengthUs
                val trackId = call.body.getOrNull(0) as? String
                val position = call.body.getOrNull(1) as? Long
                val onTrack = state != null && trackId == state.trackId
                if (onTrack && length != null && position != null && position in 0..length) {
                    commands.setPosition(position)
                }
            }
            "OpenUri" -> return bus.replyError(call, NOT_SUPPORTED, "The player opens nothing from the shell")
            else -> return bus.replyError(call, UNKNOWN_METHOD, "No method $member")
        }
        bus.reply(call)
    }

    private fun answerProperties(bus: DbusConnection, call: DbusMessage, member: String, shown: Timed?) {
        val properties = when (call.body.firstOrNull() as? String) {
            ROOT_INTERFACE -> MprisProperties.root(identity, desktopEntry)
            PLAYER_INTERFACE -> MprisProperties.player(
                shown?.state,
                shown?.positionAt(TimeSource.Monotonic.markNow()) ?: 0L,
            )
            else -> return bus.replyError(call, UNKNOWN_INTERFACE, "No interface ${call.body.firstOrNull()}")
        }
        when (member) {
            "Get" -> {
                val name = call.body.getOrNull(1) as? String
                val value = properties[name] ?: return bus.replyError(call, UNKNOWN_PROPERTY, "No property $name")
                bus.reply(call, "v", value)
            }
            "GetAll" -> bus.reply(call, "a{sv}", properties)
            "Set" -> bus.replyError(call, PROPERTY_READ_ONLY, "The player's properties are read-only")
            else -> bus.replyError(call, UNKNOWN_METHOD, "No method $member")
        }
    }

    private fun answerPeer(bus: DbusConnection, call: DbusMessage, member: String) {
        when (member) {
            "Ping" -> bus.reply(call)
            "GetMachineId" -> {
                val id = MACHINE_ID_FILES.firstNotNullOfOrNull { file ->
                    runCatching { Files.readString(file).trim() }.getOrNull()?.takeIf(String::isNotEmpty)
                }
                if (id != null) bus.reply(call, "s", id) else bus.replyError(call, FAILED, "No machine ID")
            }
            else -> bus.replyError(call, UNKNOWN_METHOD, "No method $member")
        }
    }

    private class Timed(val state: MprisPlayerState, val at: TimeSource.Monotonic.ValueTimeMark) {
        fun positionAt(mark: TimeSource.Monotonic.ValueTimeMark): Long {
            if (!state.playing) return state.positionUs
            val played = ((mark - at).inWholeMicroseconds * state.rate).toLong()
            val position = state.positionUs + played
            return state.lengthUs?.let { position.coerceAtMost(it) } ?: position
        }
    }

    private companion object {
        const val PATH = "/org/mpris/MediaPlayer2"
        const val PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties"
        const val INTROSPECTABLE_INTERFACE = "org.freedesktop.DBus.Introspectable"
        const val PEER_INTERFACE = "org.freedesktop.DBus.Peer"

        const val UNKNOWN_OBJECT = "org.freedesktop.DBus.Error.UnknownObject"
        const val UNKNOWN_INTERFACE = "org.freedesktop.DBus.Error.UnknownInterface"
        const val UNKNOWN_METHOD = "org.freedesktop.DBus.Error.UnknownMethod"
        const val UNKNOWN_PROPERTY = "org.freedesktop.DBus.Error.UnknownProperty"
        const val PROPERTY_READ_ONLY = "org.freedesktop.DBus.Error.PropertyReadOnly"
        const val NOT_SUPPORTED = "org.freedesktop.DBus.Error.NotSupported"
        const val FAILED = "org.freedesktop.DBus.Error.Failed"

        // A call may leave out its interface; each member these objects answer belongs to one
        val INTERFACE_OF_MEMBER = buildMap {
            listOf("Next", "Previous", "Pause", "PlayPause", "Stop", "Play", "Seek", "SetPosition", "OpenUri")
                .forEach { put(it, PLAYER_INTERFACE) }
            listOf("Raise", "Quit").forEach { put(it, ROOT_INTERFACE) }
            listOf("Get", "GetAll", "Set").forEach { put(it, PROPERTIES_INTERFACE) }
            put("Introspect", INTROSPECTABLE_INTERFACE)
            listOf("Ping", "GetMachineId").forEach { put(it, PEER_INTERFACE) }
        }

        val MACHINE_ID_FILES = listOf(Path.of("/etc/machine-id"), Path.of("/var/lib/dbus/machine-id"))

        // The state reports the position a few times a second while playing, so a smaller miss is only its rhythm
        const val SEEK_JUMP_US = 1_500_000L

        // Updates and closing wake the thread sooner; this only bounds how long it sleeps without either
        val IDLE_WAIT = 5.seconds
    }
}
