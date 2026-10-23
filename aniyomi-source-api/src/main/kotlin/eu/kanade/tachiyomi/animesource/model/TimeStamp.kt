package eu.kanade.tachiyomi.animesource.model

/** A marked span of a video, such as an opening; [start] and [end] are in seconds. */
data class TimeStamp(
    val start: Double,
    val end: Double,
    val name: String,
    val type: ChapterType = ChapterType.Other,
)

enum class ChapterType {
    Opening,
    Ending,
    Recap,
    MixedOp,
    Other,
}
