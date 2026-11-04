package mihon.video.engine.mpv.client

import com.sun.jna.Pointer

/**
 * mpv's event thread for one instance: waits for its events and hands them to [listener] in the terms of
 * [MpvEventListener]. The events point into memory mpv reuses at the next wait, so each is read before then.
 *
 * [stop] wakes the thread and joins it, after which the instance may be destroyed: a wait must never run on a destroyed
 * instance, and a closed session gets no more events.
 */
internal class MpvEventPump(
    private val mpv: LibMpv,
    private val client: JnaMpvClient,
    private val listener: MpvEventListener,
    name: String,
) {
    @Volatile
    private var running = true

    private val thread = Thread(::run, name).apply { isDaemon = true }

    fun start() = thread.start()

    fun stop() {
        running = false
        mpv.wakeup(client.handle)
        if (Thread.currentThread() !== thread) thread.join()
    }

    private fun run() {
        while (running) {
            val event = mpv.waitEvent(client.handle, -1.0)
            when (event.getInt(EVENT_ID)) {
                MPV_EVENT_SHUTDOWN -> return
                MPV_EVENT_LOG_MESSAGE -> logMessage(event.getPointer(EVENT_DATA))
                MPV_EVENT_END_FILE -> endFile(event.getPointer(EVENT_DATA))
                MPV_EVENT_FILE_LOADED -> listener.onFileLoaded()
                MPV_EVENT_PLAYBACK_RESTART -> listener.onPlaybackRestart(client)
                MPV_EVENT_PROPERTY_CHANGE -> propertyChange(event.getPointer(EVENT_DATA))
            }
        }
    }

    private fun propertyChange(data: Pointer) {
        val name = data.getPointer(PROPERTY_NAME).utf8()
        val value = data.getPointer(PROPERTY_DATA)
        val parsed = when {
            value == null -> MpvValue.None
            else -> when (data.getInt(PROPERTY_FORMAT)) {
                MpvFormatCode.STRING -> MpvValue.Text(value.getPointer(0).utf8())
                MpvFormatCode.FLAG -> MpvValue.Flag(value.getInt(0) != 0)
                MpvFormatCode.INT64 -> MpvValue.Int64(value.getLong(0))
                MpvFormatCode.DOUBLE -> MpvValue.Double(value.getDouble(0))
                else -> MpvValue.None
            }
        }
        listener.onPropertyChange(name, parsed)
    }

    private fun logMessage(data: Pointer) {
        val level = when (data.getInt(LOG_LEVEL)) {
            10 -> MpvLogLevel.Fatal
            20 -> MpvLogLevel.Error
            30 -> MpvLogLevel.Warn
            40 -> MpvLogLevel.Info
            50 -> MpvLogLevel.Verbose
            60 -> MpvLogLevel.Debug
            70 -> MpvLogLevel.Trace
            else -> return
        }
        listener.onLogMessage(data.getPointer(LOG_PREFIX).utf8(), level, data.getPointer(LOG_TEXT).utf8())
    }

    private fun endFile(data: Pointer) {
        val reason = when (data.getInt(END_REASON)) {
            0 -> MpvEndReason.Eof
            2 -> MpvEndReason.Stop
            3 -> MpvEndReason.Quit
            4 -> MpvEndReason.Error
            5 -> MpvEndReason.Redirect
            else -> return
        }
        val error = data.getInt(END_ERROR)
        listener.onEndFile(
            MpvEndFile(
                reason = reason,
                error = error,
                errorMessage = if (error < 0) mpv.errorString(error) else null,
                playlistEntryId = data.getLong(END_PLAYLIST_ENTRY_ID),
            ),
        )
    }

    private fun Pointer?.utf8(): String = this?.getString(0, Charsets.UTF_8.name()).orEmpty()

    private companion object {
        // mpv_event_id
        const val MPV_EVENT_SHUTDOWN = 1
        const val MPV_EVENT_LOG_MESSAGE = 2
        const val MPV_EVENT_END_FILE = 7
        const val MPV_EVENT_FILE_LOADED = 8
        const val MPV_EVENT_PLAYBACK_RESTART = 21
        const val MPV_EVENT_PROPERTY_CHANGE = 22

        // Field offsets on x86_64, the one platform the app carries libmpv for.
        // mpv_event: event_id, error, reply_userdata, data
        const val EVENT_ID = 0L
        const val EVENT_DATA = 16L

        // mpv_event_property: name, format, data
        const val PROPERTY_NAME = 0L
        const val PROPERTY_FORMAT = 8L
        const val PROPERTY_DATA = 16L

        // mpv_event_log_message: prefix, level, text, log_level
        const val LOG_PREFIX = 0L
        const val LOG_TEXT = 16L
        const val LOG_LEVEL = 24L

        // mpv_event_end_file: reason, error, playlist_entry_id
        const val END_REASON = 0L
        const val END_ERROR = 4L
        const val END_PLAYLIST_ENTRY_ID = 8L
    }
}
