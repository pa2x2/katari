package tachiyomi.domain.entry.model

/**
 * An episode's subtitles shown [delayMs] later than their file says, or earlier when negative, as set for the subtitle
 * [subtitleKey] names; never 0, which is no timing.
 */
data class SubtitleTiming(
    val chapterId: Long,
    val subtitleKey: String,
    val delayMs: Long,
)
