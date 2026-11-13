package eu.kanade.tachiyomi.ui.video.player.subtitle.timing

import kotlin.math.abs
import kotlin.math.roundToLong

/** How far a step of the timing bar or the Z and X keys moves subtitles (D003). */
internal const val SUBTITLE_TIMING_STEP_MS = 100L

/** The furthest subtitles move either way: well past any release's difference in intros and recaps. */
internal const val SUBTITLE_TIMING_LIMIT_MS = 600_000L

private const val MINUS_SIGN = '−'

/** [delayMs] on the steps' grid and within the limit, as every way of setting the timing leaves it. */
internal fun steppedSubtitleTiming(delayMs: Long): Long =
    ((delayMs.toDouble() / SUBTITLE_TIMING_STEP_MS).roundToLong() * SUBTITLE_TIMING_STEP_MS)
        .coerceIn(-SUBTITLE_TIMING_LIMIT_MS, SUBTITLE_TIMING_LIMIT_MS)

/** [delayMs] in seconds with its sign, as the timing bar shows it: "+0.4", "−1.5", "0.0". */
internal fun signedSubtitleTimingSeconds(delayMs: Long): String = when {
    delayMs > 0 -> "+${unsignedSeconds(delayMs)}"
    delayMs < 0 -> "$MINUS_SIGN${unsignedSeconds(delayMs)}"
    else -> unsignedSeconds(delayMs)
}

/** [delayMs] in seconds as the dialog's field starts with it, in the signs a keyboard types: "0.4", "-1.5". */
internal fun typedSubtitleTimingSeconds(delayMs: Long): String =
    if (delayMs < 0) "-${unsignedSeconds(delayMs)}" else unsignedSeconds(delayMs)

/** The timing typed as [text], in seconds, with either minus sign for earlier; null when it isn't a number. */
internal fun parseSubtitleTimingSeconds(text: String): Long? {
    val normalized = text.trim().replace(MINUS_SIGN, '-').replace(',', '.').removePrefix("+")
    val seconds = normalized.toDoubleOrNull()?.takeIf { it.isFinite() } ?: return null
    return steppedSubtitleTiming((seconds * 1000).roundToLong())
}

private fun unsignedSeconds(delayMs: Long): String {
    val tenths = abs(steppedSubtitleTiming(delayMs)) / SUBTITLE_TIMING_STEP_MS
    return "${tenths / 10}.${tenths % 10}"
}
