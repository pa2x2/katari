package mihon.video.engine.mpv.client

import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVLib.MpvEndFileReason
import `is`.xyz.mpv.MPVLib.MpvLogLevel as MpvLibLogLevel

/* What `MPVLib` reports, in the terms of [MpvEventListener]. `MPVLib` passes `client.h`'s values on as they are. */

/** Null for a reason Katari doesn't know. */
internal fun MPVLib.EndFile.toMpvEndFile(): MpvEndFile? {
    val reason = when (reason) {
        MpvEndFileReason.MPV_END_FILE_REASON_EOF -> MpvEndReason.Eof
        MpvEndFileReason.MPV_END_FILE_REASON_STOP -> MpvEndReason.Stop
        MpvEndFileReason.MPV_END_FILE_REASON_QUIT -> MpvEndReason.Quit
        MpvEndFileReason.MPV_END_FILE_REASON_ERROR -> MpvEndReason.Error
        MpvEndFileReason.MPV_END_FILE_REASON_REDIRECT -> MpvEndReason.Redirect
        else -> return null
    }
    return MpvEndFile(reason, error, errorMessage, playlistEntryId)
}

/** Null for none. */
internal fun mpvLogLevelOf(level: Int): MpvLogLevel? = when (level) {
    MpvLibLogLevel.MPV_LOG_LEVEL_FATAL -> MpvLogLevel.Fatal
    MpvLibLogLevel.MPV_LOG_LEVEL_ERROR -> MpvLogLevel.Error
    MpvLibLogLevel.MPV_LOG_LEVEL_WARN -> MpvLogLevel.Warn
    MpvLibLogLevel.MPV_LOG_LEVEL_INFO -> MpvLogLevel.Info
    MpvLibLogLevel.MPV_LOG_LEVEL_V -> MpvLogLevel.Verbose
    MpvLibLogLevel.MPV_LOG_LEVEL_DEBUG -> MpvLogLevel.Debug
    MpvLibLogLevel.MPV_LOG_LEVEL_TRACE -> MpvLogLevel.Trace
    else -> null
}
