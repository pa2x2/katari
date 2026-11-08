package app.katari.video.engine.mpv.audio

import com.sun.jna.Callback
import com.sun.jna.FunctionMapper
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * The part of libpulse's asynchronous API the audio output watch calls: a threaded main loop, a context on it, and the
 * server's and sinks' descriptions. Each function is the C one with the `pa_` prefix and in snake case:
 * [contextGetServerInfo] is `pa_context_get_server_info`. PipeWire answers it too, through its PulseAudio server.
 */
internal interface LibPulse : Library {
    fun threadedMainloopNew(): Pointer?

    fun threadedMainloopGetApi(mainloop: Pointer): Pointer

    fun threadedMainloopStart(mainloop: Pointer): Int

    fun threadedMainloopStop(mainloop: Pointer)

    fun threadedMainloopFree(mainloop: Pointer)

    fun threadedMainloopLock(mainloop: Pointer)

    fun threadedMainloopUnlock(mainloop: Pointer)

    fun contextNew(api: Pointer, name: String): Pointer?

    fun contextSetStateCallback(context: Pointer, callback: ContextNotifyCallback?, userdata: Pointer?)

    fun contextConnect(context: Pointer, server: String?, flags: Int, spawnApi: Pointer?): Int

    fun contextGetState(context: Pointer): Int

    fun contextSetSubscribeCallback(context: Pointer, callback: ContextSubscribeCallback?, userdata: Pointer?)

    fun contextSubscribe(context: Pointer, mask: Int, callback: Pointer?, userdata: Pointer?): Pointer?

    fun contextGetServerInfo(context: Pointer, callback: ServerInfoCallback, userdata: Pointer?): Pointer?

    fun contextGetSinkInfoByName(
        context: Pointer,
        name: String,
        callback: SinkInfoCallback,
        userdata: Pointer?,
    ): Pointer?

    fun contextDisconnect(context: Pointer)

    fun contextUnref(context: Pointer)

    fun operationUnref(operation: Pointer)

    companion object {
        /** Null where the system has no libpulse, which leaves the player without the watch. */
        val INSTANCE: LibPulse? by lazy {
            try {
                Native.load(
                    // The versioned name: the unversioned one comes only with libpulse's development files
                    "libpulse.so.0",
                    LibPulse::class.java,
                    mapOf(
                        Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method ->
                            "pa_" + method.name.replace(Regex("[A-Z]")) { "_" + it.value.lowercase() }
                        },
                        Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name(),
                    ),
                )
            } catch (_: UnsatisfiedLinkError) {
                null
            }
        }

        const val CONTEXT_NOAUTOSPAWN = 1

        const val CONTEXT_READY = 4
        const val CONTEXT_FAILED = 5
        const val CONTEXT_TERMINATED = 6

        const val SUBSCRIPTION_MASK_SINK = 0x0001
        const val SUBSCRIPTION_MASK_SERVER = 0x0080
        const val SUBSCRIPTION_EVENT_FACILITY_MASK = 0x000F
        const val SUBSCRIPTION_EVENT_SINK = 0x0000
        const val SUBSCRIPTION_EVENT_TYPE_MASK = 0x0030
        const val SUBSCRIPTION_EVENT_REMOVE = 0x0020

        /** `pa_port_available_t`: the port's jack is unplugged, or its device is otherwise gone. */
        const val PORT_AVAILABLE_NO = 1
    }
}

internal fun interface ContextNotifyCallback : Callback {
    fun invoke(context: Pointer?, userdata: Pointer?)
}

internal fun interface ContextSubscribeCallback : Callback {
    fun invoke(context: Pointer?, type: Int, index: Int, userdata: Pointer?)
}

internal fun interface ServerInfoCallback : Callback {
    fun invoke(context: Pointer?, info: Pointer?, userdata: Pointer?)
}

internal fun interface SinkInfoCallback : Callback {
    fun invoke(context: Pointer?, info: Pointer?, eol: Int, userdata: Pointer?)
}

/** `pa_server_info`, as far as the default sink's name. */
@Structure.FieldOrder("userName", "hostName", "serverVersion", "serverName", "sampleSpec", "defaultSinkName")
internal class ServerInfo(pointer: Pointer) : Structure(pointer) {
    @JvmField var userName: Pointer? = null

    @JvmField var hostName: Pointer? = null

    @JvmField var serverVersion: Pointer? = null

    @JvmField var serverName: Pointer? = null

    @JvmField var sampleSpec = SampleSpec()

    @JvmField var defaultSinkName: String? = null

    init {
        read()
    }
}

/** `pa_sink_info`, as far as its ports; the strings and ports are libpulse's, valid only inside the callback. */
@Structure.FieldOrder(
    "name", "index", "description", "sampleSpec", "channelMap", "ownerModule", "volume", "mute", "monitorSource",
    "monitorSourceName", "latency", "driver", "flags", "proplist", "configuredLatency", "baseVolume", "state",
    "nVolumeSteps", "card", "nPorts", "ports", "activePort",
)
internal class SinkInfo(pointer: Pointer) : Structure(pointer) {
    @JvmField var name: String? = null

    @JvmField var index: Int = 0

    @JvmField var description: Pointer? = null

    @JvmField var sampleSpec = SampleSpec()

    @JvmField var channelMap = ChannelValues()

    @JvmField var ownerModule: Int = 0

    @JvmField var volume = ChannelValues()

    @JvmField var mute: Int = 0

    @JvmField var monitorSource: Int = 0

    @JvmField var monitorSourceName: Pointer? = null

    @JvmField var latency: Long = 0

    @JvmField var driver: Pointer? = null

    @JvmField var flags: Int = 0

    @JvmField var proplist: Pointer? = null

    @JvmField var configuredLatency: Long = 0

    @JvmField var baseVolume: Int = 0

    @JvmField var state: Int = 0

    @JvmField var nVolumeSteps: Int = 0

    @JvmField var card: Int = 0

    @JvmField var nPorts: Int = 0

    /** `pa_sink_port_info **`, [nPorts] of them. */
    @JvmField var ports: Pointer? = null

    @JvmField var activePort: Pointer? = null

    init {
        read()
    }
}

/** `pa_sample_spec`. */
@Structure.FieldOrder("format", "rate", "channels")
internal class SampleSpec : Structure() {
    @JvmField var format: Int = 0

    @JvmField var rate: Int = 0

    @JvmField var channels: Byte = 0
}

/** `pa_channel_map` and `pa_cvolume`, alike in layout: a channel count, then 32 four-byte values. */
@Structure.FieldOrder("channels", "values")
internal class ChannelValues : Structure() {
    @JvmField var channels: Byte = 0

    @JvmField var values = IntArray(CHANNELS_MAX)

    private companion object {
        const val CHANNELS_MAX = 32
    }
}

/** A sink's port, from a `pa_sink_port_info *`: its name, and whether it can play now ([LibPulse.PORT_AVAILABLE_NO]). */
internal class SinkPort(val name: String, val available: Int) {
    companion object {
        // pa_sink_port_info: name, description, then priority and available, both 32-bit
        private val AVAILABLE_OFFSET = Native.POINTER_SIZE * 2L + Int.SIZE_BYTES

        fun of(pointer: Pointer): SinkPort = SinkPort(
            name = pointer.getPointer(0).getString(0, Charsets.UTF_8.name()),
            available = pointer.getInt(AVAILABLE_OFFSET),
        )
    }
}
