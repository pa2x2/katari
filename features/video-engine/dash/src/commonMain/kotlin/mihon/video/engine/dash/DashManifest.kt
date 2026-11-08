package mihon.video.engine.dash

import okhttp3.HttpUrl
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * A DASH manifest as [readDashManifest] read it: what each representation is and where its media lives, with the
 * `BaseURL` chain and inherited addressing already resolved. The parsed document is kept so [localDashManifest] can
 * edit it and write it back, keeping everything this model doesn't name (timing, roles, event streams).
 */
class DashManifest internal constructor(
    internal val document: Document,
    /** A live manifest (`type="dynamic"`), whose segments keep appearing, so there is no whole to download. */
    val isDynamic: Boolean,
    /** Some of its elements live elsewhere (`xlink:href`), unread. */
    val hasRemoteElements: Boolean,
    val periods: List<DashPeriod>,
)

class DashPeriod internal constructor(
    internal val element: Element,
    /** Null when neither the period nor the manifest says how long it lasts. */
    val durationSeconds: Double?,
    val adaptationSets: List<DashAdaptationSet>,
)

class DashAdaptationSet internal constructor(
    internal val element: Element,
    val contentType: DashContentType,
    val language: String?,
    val label: String?,
    val roles: List<String>,
    /** Key frames for fast seeking, not a video to watch. */
    val isTrickPlay: Boolean,
    val representations: List<DashRepresentation>,
)

class DashRepresentation internal constructor(
    internal val element: Element,
    val id: String,
    val bandwidth: Long,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val codecs: String?,
    /** It or its set names a `ContentProtection` scheme: its media is encrypted. */
    val isProtected: Boolean,
    internal val baseUrl: HttpUrl,
    internal val addressing: DashAddressing,
    internal val periodDurationSeconds: Double?,
) {
    /** Its size at its declared bandwidth, a peak, over its period; null when the period's length is unknown. */
    val estimatedBytes: Long?
        get() = periodDurationSeconds?.let { (bandwidth * it / 8).toLong() }
}

enum class DashContentType { VIDEO, AUDIO, TEXT, IMAGE, OTHER }

/**
 * How a representation's media is split into files, with the attributes and children it inherits from its set and
 * period merged into one element.
 */
internal sealed interface DashAddressing {
    /** One file at the representation's base URL; [segmentBase], if any, indexes it by byte ranges. */
    data class SingleFile(val segmentBase: Element?) : DashAddressing

    data class Template(val segmentTemplate: Element) : DashAddressing

    data class SegmentList(val segmentList: Element) : DashAddressing
}

/** A manifest this reader can't follow, so its media can't be listed. */
class UnsupportedDashManifestException(message: String) : UnsupportedOperationException(message)
