package eu.kanade.tachiyomi.ui.video.player.markers

import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import mihon.video.engine.spi.chapters.FileChapter
import kotlin.math.abs

/** One part of the episode the player names and lists, from [startMs] up to [endMs]. */
internal data class VideoPlayerChapter(
    val startMs: Long,
    val endMs: Long,
    val name: VideoPlayerChapterName,
)

internal sealed interface VideoPlayerChapterName {
    data class Given(val text: String) : VideoPlayerChapterName

    /** An intro, recap, credits or preview the source marks without a name of its own. */
    data class Marker(val kind: PlaybackMarkerKind) : VideoPlayerChapterName

    /** A part nothing names, numbered by its place in the list from 1. */
    data class Numbered(val number: Int) : VideoPlayerChapterName
}

/**
 * The episode's chapters, laid end to end from the start to [durationMs] so every position falls in one. Empty while
 * the duration is unknown, and when neither the source nor the file names any chapter: intro and credits markers alone
 * are left to the skip button.
 *
 * When the source names chapters, its markers are the list and the file's chapters are left out, so the list is the
 * same on every engine. Otherwise the file's chapters are, with the source's intro, recap, credits and preview laid
 * over them. Their times come from the source, which is often a second or two off the file's, so a file chapter that
 * starts within [SNAP_MS] of a marker's edge moves onto it, and one that starts with a marker becomes that marker,
 * under the file's name. A marker inside a chapter splits it, and a stretch no marker or chapter covers is a part of
 * its own when it is longer than [SNAP_MS].
 */
internal fun videoPlayerChapters(
    markers: List<PlaybackMarker>,
    fileChapters: List<FileChapter>,
    durationMs: Long,
): List<VideoPlayerChapter> {
    if (durationMs <= 0L) return emptyList()
    val sourceNamesChapters = markers.any { it.kind == PlaybackMarkerKind.CHAPTER }
    if (!sourceNamesChapters && fileChapters.isEmpty()) return emptyList()

    // Every span belongs to an owner, which names the parts it ends up covering; a null name is numbered.
    val names = mutableListOf<VideoPlayerChapterName?>()
    val spans = mutableListOf<Span>()
    val markerOwners = markers.map { marker ->
        names += marker.chapterName()
        val owner = names.lastIndex
        spans += Span(marker.startMs, marker.endMs, marker.priority, owner)
        marker to owner
    }
    if (!sourceNamesChapters) {
        val edges = buildList {
            add(0L)
            add(durationMs)
            markers.forEach {
                add(it.startMs)
                add(it.endMs)
            }
        }
        val starts = fileChapters
            .map { chapter ->
                val nearestEdge = edges.minBy { abs(it - chapter.startMs) }
                chapter to if (abs(nearestEdge - chapter.startMs) <= SNAP_MS) nearestEdge else chapter.startMs
            }
            .sortedBy { (_, startMs) -> startMs }
            .distinctBy { (_, startMs) -> startMs }
        starts.forEachIndexed { index, (chapter, startMs) ->
            val title = chapter.title?.let(VideoPlayerChapterName::Given)
            val folded = markerOwners.firstOrNull { (marker, _) -> marker.startMs == startMs }?.second
            val owner = if (folded != null) {
                if (title != null) names[folded] = title
                folded
            } else {
                names += title
                names.lastIndex
            }
            val endMs = starts.getOrNull(index + 1)?.second ?: durationMs
            spans += Span(startMs, endMs, CHAPTER_PRIORITY, owner)
        }
    }

    val cuts = (spans.flatMap { listOf(it.startMs, it.endMs) } + 0L + durationMs)
        .map { it.coerceIn(0L, durationMs) }
        .distinct()
        .sorted()
    val parts = mutableListOf<Part>()
    cuts.zipWithNext { startMs, endMs ->
        val owner = spans
            .filter { it.startMs <= startMs && it.endMs >= endMs }
            .maxWithOrNull(compareBy<Span>({ it.priority }, { it.startMs }))
            ?.owner
        val previous = parts.lastOrNull()
        if (previous != null && previous.owner == owner) {
            parts[parts.lastIndex] = previous.copy(endMs = endMs)
        } else {
            parts += Part(startMs, endMs, owner)
        }
    }
    // A sliver nothing covers is where the source's and the file's times disagree slightly, not a part of its own.
    val merged = mutableListOf<Part>()
    parts.forEach { part ->
        val previous = merged.lastOrNull()
        when {
            part.owner != null || part.endMs - part.startMs > SNAP_MS -> merged += part
            previous != null -> merged[merged.lastIndex] = previous.copy(endMs = part.endMs)
            else -> merged += part
        }
    }
    val first = merged.first()
    if (first.owner == null && merged.size > 1 && first.endMs <= SNAP_MS) {
        merged.removeAt(0)
        merged[0] = merged[0].copy(startMs = 0L)
    }

    return merged.mapIndexed { index, part ->
        VideoPlayerChapter(
            startMs = part.startMs,
            endMs = part.endMs,
            name = part.owner?.let { names[it] } ?: VideoPlayerChapterName.Numbered(index + 1),
        )
    }
}

/** The chapter [positionMs] falls in, or null when there are none. */
internal fun List<VideoPlayerChapter>.chapterAt(positionMs: Long): VideoPlayerChapter? =
    lastOrNull { it.startMs <= positionMs }

private data class Span(val startMs: Long, val endMs: Long, val priority: Int, val owner: Int)

private data class Part(val startMs: Long, val endMs: Long, val owner: Int?)

private fun PlaybackMarker.chapterName(): VideoPlayerChapterName? =
    label?.takeIf(String::isNotBlank)?.let(VideoPlayerChapterName::Given)
        ?: VideoPlayerChapterName.Marker(kind).takeIf { kind != PlaybackMarkerKind.CHAPTER }

// Where spans overlap, a skippable marker wins over the chapter it sits in; among equals, the later and inner one.
private val PlaybackMarker.priority: Int
    get() = if (skippable) SKIPPABLE_PRIORITY else CHAPTER_PRIORITY

private const val CHAPTER_PRIORITY = 0
private const val SKIPPABLE_PRIORITY = 1

private const val SNAP_MS = 3_000L
