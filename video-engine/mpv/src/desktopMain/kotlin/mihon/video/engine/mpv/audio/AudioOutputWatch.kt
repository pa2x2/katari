package mihon.video.engine.mpv.audio

import com.sun.jna.Pointer
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Tells [onOutputLost] when the device the desktop plays its sound on goes away, as Android's "becoming noisy" does:
 * headphones unplugged from their jack, a Bluetooth or USB headset disconnected. mpv follows the desktop's default
 * output and says nothing when it changes, and an unplugged jack only switches the sound card's port, so the watch asks
 * the sound server itself, PulseAudio or PipeWire's PulseAudio server, through libpulse.
 *
 * The output is the default sink and its active port. It is lost when that sink is removed, or when the sink switches
 * to another port because the one it played on is no longer available; choosing another output, or plugging
 * headphones in, loses nothing. A sink removed while it was the default is reported once, either by its removal or by
 * the default moving off a sink that no longer exists. Without libpulse or a sound server it never calls.
 * [onOutputLost] comes on libpulse's thread.
 */
internal class AudioOutputWatch(private val onOutputLost: () -> Unit) {
    private val pulse = LibPulse.INSTANCE
    private var mainloop: Pointer? = null
    private var context: Pointer? = null

    // Confined to libpulse's thread, where every callback comes.
    private var output: Output? = null

    // Held here, as libpulse keeps only their addresses.
    private val stateCallback = ContextNotifyCallback { context, _ -> context?.let(::onStateChanged) }
    private val subscribeCallback = ContextSubscribeCallback { context, type, index, _ ->
        if (context != null) onEvent(context, type, index)
    }
    private val serverInfoCallback = ServerInfoCallback { context, info, _ ->
        if (context != null && info != null) onServerInfo(context, ServerInfo(info))
    }
    private val sinkInfoCallback = SinkInfoCallback { context, info, eol, _ ->
        if (context != null && eol == 0 && info != null) onSinkInfo(context, SinkInfo(info))
    }

    // Whether the sink played on before the default changed still exists: the server may move the default away before
    // it reports the sink removed, or report no removal at all.
    private var formerSinkFound = false
    private val formerSinkCallback = SinkInfoCallback { _, info, eol, _ ->
        when {
            eol == 0 && info != null -> formerSinkFound = true
            eol != 0 && !formerSinkFound -> onOutputLost()
        }
    }

    fun start() {
        val pulse = pulse ?: return logcat(LogPriority.INFO) { "No libpulse to watch the sound output through" }
        val mainloop = pulse.threadedMainloopNew() ?: return
        val context = pulse.contextNew(pulse.threadedMainloopGetApi(mainloop), CLIENT_NAME)
        if (context == null) {
            pulse.threadedMainloopFree(mainloop)
            return
        }
        this.mainloop = mainloop
        this.context = context
        pulse.contextSetStateCallback(context, stateCallback, null)
        if (pulse.contextConnect(context, null, LibPulse.CONTEXT_NOAUTOSPAWN, null) < 0) {
            logcat(LogPriority.INFO) { "No sound server to watch the output on" }
            release()
            return
        }
        pulse.threadedMainloopStart(mainloop)
    }

    fun release() {
        val pulse = pulse ?: return
        val mainloop = mainloop ?: return
        val context = context
        pulse.threadedMainloopLock(mainloop)
        if (context != null) {
            pulse.contextSetSubscribeCallback(context, null, null)
            pulse.contextSetStateCallback(context, null, null)
            pulse.contextDisconnect(context)
            pulse.contextUnref(context)
        }
        pulse.threadedMainloopUnlock(mainloop)
        pulse.threadedMainloopStop(mainloop)
        pulse.threadedMainloopFree(mainloop)
        this.mainloop = null
        this.context = null
    }

    private fun onStateChanged(context: Pointer) {
        val pulse = pulse ?: return
        when (pulse.contextGetState(context)) {
            LibPulse.CONTEXT_READY -> {
                pulse.contextSetSubscribeCallback(context, subscribeCallback, null)
                pulse.contextSubscribe(
                    context,
                    LibPulse.SUBSCRIPTION_MASK_SINK or LibPulse.SUBSCRIPTION_MASK_SERVER,
                    null,
                    null,
                )?.let(pulse::operationUnref)
                readOutput(context)
            }
            LibPulse.CONTEXT_FAILED, LibPulse.CONTEXT_TERMINATED -> output = null
        }
    }

    private fun onEvent(context: Pointer, type: Int, index: Int) {
        val facility = type and LibPulse.SUBSCRIPTION_EVENT_FACILITY_MASK
        val removed = type and LibPulse.SUBSCRIPTION_EVENT_TYPE_MASK == LibPulse.SUBSCRIPTION_EVENT_REMOVE
        if (facility == LibPulse.SUBSCRIPTION_EVENT_SINK && removed && index == output?.sinkIndex) {
            output = null
            onOutputLost()
        }
        readOutput(context)
    }

    private fun readOutput(context: Pointer) {
        val pulse = pulse ?: return
        pulse.contextGetServerInfo(context, serverInfoCallback, null)?.let(pulse::operationUnref)
    }

    private fun onServerInfo(context: Pointer, info: ServerInfo) {
        val pulse = pulse ?: return
        val sink = info.defaultSinkName ?: return
        pulse.contextGetSinkInfoByName(context, sink, sinkInfoCallback, null)?.let(pulse::operationUnref)
    }

    private fun onSinkInfo(context: Pointer, info: SinkInfo) {
        val name = info.name ?: return
        val ports = info.ports
            ?.getPointerArray(0, info.nPorts)
            ?.map(SinkPort::of)
            .orEmpty()
        val activePort = info.activePort?.let(SinkPort::of)?.name
        val previous = output
        output = Output(name, info.index, activePort)
        val portGone = previous != null &&
            previous.sinkName == name &&
            previous.port != null &&
            previous.port != activePort &&
            ports.firstOrNull { it.name == previous.port }?.available == LibPulse.PORT_AVAILABLE_NO
        if (portGone) onOutputLost()
        if (previous != null && previous.sinkName != name) {
            val pulse = pulse ?: return
            formerSinkFound = false
            pulse.contextGetSinkInfoByName(
                context,
                previous.sinkName,
                formerSinkCallback,
                null,
            )?.let(pulse::operationUnref)
        }
    }

    private data class Output(val sinkName: String, val sinkIndex: Int, val port: String?)

    private companion object {
        const val CLIENT_NAME = "Katari"
    }
}
