package app.katari.video.engine.mpv.client

/** What an mpv instance reports, on its event thread, which must not block. */
interface MpvEventListener {
    /** An observed property changed to [value]: one of the type it is observed as, or [MpvValue.None]. */
    fun onPropertyChange(property: String, value: MpvValue)

    fun onFileLoaded()

    /**
     * Playback started again, after a load or a seek. [client] reads the instance as it is at this moment, which the
     * property changes reported after it no longer show.
     */
    fun onPlaybackRestart(client: MpvClient)

    fun onEndFile(end: MpvEndFile)

    fun onLogMessage(prefix: String, level: MpvLogLevel, text: String)
}

sealed interface MpvValue {
    /** The property has no value now, such as `duration` once the file is gone. */
    data object None : MpvValue

    data class Text(val value: String) : MpvValue

    data class Flag(val value: Boolean) : MpvValue

    data class Int64(val value: Long) : MpvValue

    data class Double(val value: kotlin.Double) : MpvValue
}

/**
 * A playlist entry ended. [error] is one of [MpvErrorCode] when [reason] is [MpvEndReason.Error]; [playlistEntryId]
 * names the entry, so the end of an earlier load can be told apart.
 */
data class MpvEndFile(
    val reason: MpvEndReason,
    val error: Int,
    val errorMessage: String?,
    val playlistEntryId: Long,
)

enum class MpvEndReason {
    Eof,
    Stop,
    Quit,
    Error,

    /** The file was a playlist, whose entries mpv put in its place. */
    Redirect,
}

/** Log levels from the most severe; mpv sends only those up to the level the client asked for. */
enum class MpvLogLevel {
    Fatal,
    Error,
    Warn,
    Info,
    Verbose,
    Debug,
    Trace,
}

/** `client.h`'s `mpv_error` codes that Katari tells apart. */
object MpvErrorCode {
    const val LOADING_FAILED = -13
    const val AO_INIT_FAILED = -14
    const val VO_INIT_FAILED = -15
    const val NOTHING_TO_PLAY = -16
    const val UNKNOWN_FORMAT = -17
    const val UNSUPPORTED = -18
    const val NOT_IMPLEMENTED = -19
}
