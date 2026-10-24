package mihon.video.engine.dash

import okhttp3.HttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

/**
 * Reads the manifest [text] fetched from [url], the URL it came from after redirects, which every relative
 * `BaseURL` and segment reference resolves against.
 *
 * jsoup's XML parser resolves no DTDs or external entities, so a manifest can't make the reader open files or URLs.
 */
fun readDashManifest(text: String, url: HttpUrl): DashManifest {
    val document = Jsoup.parse(text.removePrefix(BYTE_ORDER_MARK), "", Parser.xmlParser())
    val mpd = document.children().firstOrNull { it.localName == "MPD" }
        ?: throw UnsupportedDashManifestException("No MPD element")
    val mpdBase = baseUrlOf(mpd, url)
    val presentationSeconds = mpd.attrOrNull("mediaPresentationDuration")?.let(::parseXsDuration)

    val periodElements = mpd.childrenNamed("Period")
    val starts = periodStarts(periodElements)
    val periods = periodElements.mapIndexed { index, period ->
        val start = starts[index]
        val duration = period.attrOrNull("duration")?.let(::parseXsDuration)
            ?: starts.getOrNull(index + 1)?.let { next -> start?.let { next - it } }
            ?: presentationSeconds?.let { total -> total - (start ?: 0.0) }
        readPeriod(period, baseUrlOf(period, mpdBase), duration)
    }
    return DashManifest(
        document = document,
        isDynamic = mpd.attrOrNull("type") == "dynamic",
        hasRemoteElements = document.allElements.any { element ->
            element.attributes().any { it.key.endsWith(":href") }
        },
        periods = periods,
    )
}

/** Each period's start in seconds: its own `start`, else where the one before it ends. */
private fun periodStarts(periods: List<Element>): List<Double?> {
    val starts = ArrayList<Double?>(periods.size)
    periods.forEachIndexed { index, period ->
        val previous = periods.getOrNull(index - 1)
        starts += period.attrOrNull("start")?.let(::parseXsDuration)
            ?: if (index == 0) {
                0.0
            } else {
                val previousStart = starts[index - 1]
                val previousDuration = previous?.attrOrNull("duration")?.let(::parseXsDuration)
                if (previousStart != null && previousDuration != null) previousStart + previousDuration else null
            }
    }
    return starts
}

private fun readPeriod(period: Element, base: HttpUrl, durationSeconds: Double?): DashPeriod {
    val sets = period.childrenNamed("AdaptationSet").map { set ->
        val setBase = baseUrlOf(set, base)
        val representations = set.childrenNamed("Representation")
        val setProtected = set.firstChildNamed("ContentProtection") != null
        DashAdaptationSet(
            element = set,
            contentType = contentTypeOf(set, representations),
            language = set.attrOrNull("lang") ?: representations.firstNotNullOfOrNull { it.attrOrNull("lang") },
            label = set.firstChildNamed("Label")?.text()?.trim()?.ifEmpty { null },
            roles = set.childrenNamed("Role").mapNotNull { it.attrOrNull("value") },
            isTrickPlay = set.childrenNamed("EssentialProperty").any { it.attrOrNull("schemeIdUri") == TRICK_MODE },
            representations = representations.map { representation ->
                DashRepresentation(
                    element = representation,
                    id = representation.attrOrNull("id").orEmpty(),
                    bandwidth = representation.longAttr("bandwidth") ?: 0L,
                    width = (representation.attrOrNull("width") ?: set.attrOrNull("width"))?.toIntOrNull(),
                    height = (representation.attrOrNull("height") ?: set.attrOrNull("height"))?.toIntOrNull(),
                    mimeType = representation.attrOrNull("mimeType") ?: set.attrOrNull("mimeType"),
                    codecs = representation.attrOrNull("codecs") ?: set.attrOrNull("codecs"),
                    isProtected = setProtected || representation.firstChildNamed("ContentProtection") != null,
                    baseUrl = baseUrlOf(representation, setBase),
                    addressing = addressingOf(listOf(period, set, representation)),
                    periodDurationSeconds = durationSeconds,
                )
            },
        )
    }
    return DashPeriod(period, durationSeconds, sets)
}

/** The element's first `BaseURL` resolved against its parent's base, or the parent's base when it has none. */
private fun baseUrlOf(element: Element, parentBase: HttpUrl): HttpUrl {
    val reference = element.firstChildNamed("BaseURL")?.text()?.trim()?.ifEmpty { null } ?: return parentBase
    return parentBase.resolve(reference) ?: throw UnsupportedDashManifestException("Unusable BaseURL '$reference'")
}

/**
 * The addressing the lowest level of [levels] (period, set, representation) that has one names, merged with the
 * elements of the same kind above it: the lower level's attributes and children win, the rest are inherited.
 */
private fun addressingOf(levels: List<Element>): DashAddressing {
    val kind = levels.reversed().firstNotNullOfOrNull { level ->
        ADDRESSING_ELEMENTS.firstOrNull { level.firstChildNamed(it) != null }
    } ?: return DashAddressing.SingleFile(segmentBase = null)
    val chain = levels.mapNotNull { it.firstChildNamed(kind) }
    val merged = chain.last().clone()
    chain.dropLast(1).reversed().forEach { inherited ->
        inherited.attributes().forEach { if (!merged.hasAttr(it.key)) merged.attr(it.key, it.value) }
        inherited.children()
            .filter { child -> merged.firstChildNamed(child.localName) == null }
            .forEach { merged.appendChild(it.clone()) }
    }
    return when (kind) {
        "SegmentTemplate" -> DashAddressing.Template(merged)
        "SegmentList" -> DashAddressing.SegmentList(merged)
        else -> DashAddressing.SingleFile(merged)
    }
}

private fun contentTypeOf(set: Element, representations: List<Element>): DashContentType {
    val declared = set.attrOrNull("contentType")
        ?: set.childrenNamed("ContentComponent").firstNotNullOfOrNull { it.attrOrNull("contentType") }
    if (declared != null) return contentTypeNamed(declared)
    val mimeType = (set.attrOrNull("mimeType") ?: representations.firstNotNullOfOrNull { it.attrOrNull("mimeType") })
        .orEmpty().lowercase()
    val codec = (set.attrOrNull("codecs") ?: representations.firstNotNullOfOrNull { it.attrOrNull("codecs") })
        .orEmpty().lowercase()
    return when {
        TEXT_CODECS.any(codec::startsWith) || "ttml" in mimeType || mimeType.startsWith("text/") -> DashContentType.TEXT
        mimeType.startsWith("video/") || VIDEO_CODECS.any(codec::startsWith) -> DashContentType.VIDEO
        mimeType.startsWith("audio/") || AUDIO_CODECS.any(codec::startsWith) -> DashContentType.AUDIO
        mimeType.startsWith("image/") -> DashContentType.IMAGE
        else -> DashContentType.OTHER
    }
}

private fun contentTypeNamed(name: String): DashContentType = when (name.lowercase()) {
    "video" -> DashContentType.VIDEO
    "audio" -> DashContentType.AUDIO
    "text" -> DashContentType.TEXT
    "image" -> DashContentType.IMAGE
    else -> DashContentType.OTHER
}

private const val BYTE_ORDER_MARK = ""
private const val TRICK_MODE = "http://dashif.org/guidelines/trickmode"
private val TEXT_CODECS = listOf("wvtt", "stpp", "ttml")
private val VIDEO_CODECS = listOf("avc", "hev1", "hvc1", "vp08", "vp09", "vp8", "vp9", "av01", "dvh")
private val AUDIO_CODECS = listOf("mp4a", "opus", "ac-3", "ec-3", "ac-4", "flac", "vorbis", "dts")
