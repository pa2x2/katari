package mihon.video.engine.dash

import okhttp3.HttpUrl
import org.jsoup.nodes.Element
import kotlin.math.ceil

/** A file of a representation as the server has it: all of [url], or the bytes [range] of it. */
class DashSegment internal constructor(val url: HttpUrl, val range: LongRange?)

/**
 * The files a representation's media is in: an initialization segment, if it has its own, then the media segments
 * in order, each with the number a `$Number$` template gives it.
 */
internal class RepresentationSegments(
    val initialization: DashSegment?,
    val media: List<NumberedSegment>,
)

internal class NumberedSegment(val segment: DashSegment, val number: Long)

internal fun DashRepresentation.segments(): RepresentationSegments = when (val addressing = addressing) {
    is DashAddressing.SingleFile -> RepresentationSegments(
        // Without its own sourceURL the initialization is a range of the file, which stays valid once it's whole.
        initialization = addressing.segmentBase?.firstChildNamed("Initialization")
            ?.takeIf { it.attrOrNull("sourceURL") != null }
            ?.let(::initializationSegment),
        media = listOf(NumberedSegment(DashSegment(baseUrl, range = null), number = 1)),
    )
    is DashAddressing.Template -> templateSegments(addressing.segmentTemplate)
    is DashAddressing.SegmentList -> listSegments(addressing.segmentList)
}

private fun DashRepresentation.templateSegments(template: Element): RepresentationSegments {
    val media = template.attrOrNull("media")
        ?: throw UnsupportedDashManifestException("SegmentTemplate without media in representation '$id'")
    val startNumber = template.longAttr("startNumber") ?: 1L
    val timeline = template.firstChildNamed("SegmentTimeline")
    val times: List<Long?> = if (timeline != null) {
        timelineStarts(timeline, template)
    } else {
        List(durationSegmentCount(template, startNumber)) { null }
    }
    return RepresentationSegments(
        initialization = template.attrOrNull("initialization")
            ?.let { DashSegment(resolve(expandSegmentTemplate(it, this, number = null, time = null)), range = null) }
            ?: template.firstChildNamed("Initialization")?.let(::initializationSegment),
        media = times.mapIndexed { index, time ->
            val number = startNumber + index
            NumberedSegment(DashSegment(resolve(expandSegmentTemplate(media, this, number, time)), null), number)
        },
    )
}

/** The count of `@duration` segments: up to `@endNumber`, else enough to cover the period, the last one partial. */
private fun DashRepresentation.durationSegmentCount(template: Element, startNumber: Long): Int {
    template.longAttr("endNumber")?.let { return (it - startNumber + 1).coerceAtLeast(0).toInt() }
    val duration = template.longAttr("duration")?.takeIf { it > 0 }
        ?: throw UnsupportedDashManifestException("SegmentTemplate without duration or timeline in '$id'")
    val timescale = template.longAttr("timescale") ?: 1L
    val periodSeconds = periodDurationSeconds
        ?: throw UnsupportedDashManifestException("Period of unknown length in '$id'")
    return ceil(periodSeconds * timescale / duration - ROUNDING_SLACK).toInt()
}

/**
 * The start time of every segment in [timeline]: an `S` element's `t`, else where the previous one ended, repeated
 * `r` more times; `r="-1"` repeats up to the next `S`'s `t`, else to the end of the period.
 */
private fun DashRepresentation.timelineStarts(timeline: Element, template: Element): List<Long> {
    val timescale = template.longAttr("timescale") ?: 1L
    val periodEnd = periodDurationSeconds?.let {
        (template.longAttr("presentationTimeOffset") ?: 0L) + (it * timescale).toLong()
    }
    val entries = timeline.childrenNamed("S")
    val starts = ArrayList<Long>()
    var next = 0L
    entries.forEachIndexed { index, entry ->
        val start = entry.longAttr("t") ?: next
        val duration = entry.longAttr("d")?.takeIf { it > 0 }
            ?: throw UnsupportedDashManifestException("SegmentTimeline entry without duration in '$id'")
        val repeat = entry.longAttr("r") ?: 0L
        val count = if (repeat >= 0) {
            repeat + 1
        } else {
            val end = entries.getOrNull(index + 1)?.longAttr("t") ?: periodEnd
                ?: throw UnsupportedDashManifestException("Open-ended SegmentTimeline in '$id'")
            ceil((end - start).toDouble() / duration).toLong().coerceAtLeast(0)
        }
        for (k in 0 until count) starts += start + k * duration
        next = start + count * duration
    }
    return starts
}

private fun DashRepresentation.listSegments(list: Element): RepresentationSegments = RepresentationSegments(
    initialization = list.firstChildNamed("Initialization")?.let(::initializationSegment),
    media = list.childrenNamed("SegmentURL").mapIndexed { index, segment ->
        val url = segment.attrOrNull("media")?.let(::resolve) ?: baseUrl
        NumberedSegment(DashSegment(url, segment.attrOrNull("mediaRange")?.let(::parseByteRange)), index + 1L)
    },
)

private fun DashRepresentation.initializationSegment(initialization: Element): DashSegment = DashSegment(
    url = initialization.attrOrNull("sourceURL")?.let(::resolve) ?: baseUrl,
    range = initialization.attrOrNull("range")?.let(::parseByteRange),
)

private fun DashRepresentation.resolve(reference: String): HttpUrl =
    baseUrl.resolve(reference) ?: throw UnsupportedDashManifestException("Unusable segment URL '$reference'")

private fun parseByteRange(text: String): LongRange {
    val first = text.substringBefore('-').toLongOrNull()
    val last = text.substringAfter('-', "").toLongOrNull()
    if (first == null || last == null || last < first) throw UnsupportedDashManifestException("Byte range '$text'")
    return first..last
}

// A period of 634.566 s cut into 4.0107 s segments has 159, the last one partial; a duration that divides the period
// exactly mustn't gain an empty one through floating point.
private const val ROUNDING_SLACK = 1e-9
