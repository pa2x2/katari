package mihon.video.engine.dash

import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** A manifest that plays from the [files] beside it, under their names. */
class LocalDashManifest(val text: String, val files: List<LocalDashFile>)

/** What to download, [segment], and the name the local manifest gives it. */
class LocalDashFile(val segment: DashSegment, val name: String)

/**
 * [manifest] rewritten to play only the [kept] representations, from files named after the key each is given:
 * `<key>.<ext>` for a representation in one file, else `<key>-init.<ext>` and `<key>-<number>.<ext>`.
 *
 * The rest of the document stays as the server wrote it: timing, codecs, languages, roles and labels. Every `BaseURL`
 * goes, so nothing sends the player back to the network. Each kept representation carries its complete addressing,
 * with what it inherited merged in, and the set and period ones go; a `SegmentTemplate` keeps its timeline and numbering
 * with a `$Number$` media name of its own. It edits the manifest's document, so [manifest] is used up.
 */
fun localDashManifest(manifest: DashManifest, kept: Map<DashRepresentation, String>): LocalDashManifest {
    val files = ArrayList<LocalDashFile>()
    val addressing = kept.mapValues { (representation, key) -> localAddressing(representation, key, files) }

    manifest.document.allElements
        .filter { it.localName in NETWORK_ELEMENTS }
        .forEach { it.removeWithIndent() }
    manifest.periods.forEach { period ->
        period.element.removeAddressing()
        period.adaptationSets.forEach { set ->
            if (set.representations.none { it in kept }) {
                set.element.removeWithIndent()
                return@forEach
            }
            set.element.removeAddressing()
            set.representations.forEach { representation ->
                val local = addressing[representation]
                if (local == null) {
                    representation.element.removeWithIndent()
                } else {
                    representation.element.removeAddressing()
                    local.appendTo(representation.element)
                }
            }
        }
    }

    return LocalDashManifest(manifest.document.toXml(), files)
}

/** A kept representation's new children: a `BaseURL` naming its single file, and its merged addressing element. */
private class LocalAddressing(val fileName: String?, val element: Element?) {
    fun appendTo(representation: Element) {
        fileName?.let { representation.appendElement("${representation.namespacePrefix}BaseURL").text(it) }
        element?.let(representation::appendChild)
    }
}

private fun localAddressing(
    representation: DashRepresentation,
    key: String,
    files: MutableList<LocalDashFile>,
): LocalAddressing {
    val segments = representation.segments()
    val extension = fileExtension(representation.mimeType)
    val segmentExtension = if (extension == "mp4") "m4s" else extension
    val initializationName = segments.initialization?.let { segment ->
        "$key-init.$extension".also { files += LocalDashFile(segment, it) }
    }
    return when (val addressing = representation.addressing) {
        is DashAddressing.SingleFile -> {
            val name = "$key.$extension"
            files += LocalDashFile(segments.media.single().segment, name)
            val segmentBase = addressing.segmentBase?.clone()?.also { base ->
                base.childrenNamed("RepresentationIndex").forEach(Element::remove)
                initializationName?.let { name ->
                    base.firstChildNamed("Initialization")?.apply {
                        removeAttr("range")
                        attr("sourceURL", name)
                    }
                }
            }
            LocalAddressing(fileName = name, element = segmentBase)
        }
        is DashAddressing.Template -> {
            segments.media.forEach { files += LocalDashFile(it.segment, "$key-${it.number}.$segmentExtension") }
            val template = addressing.segmentTemplate.clone()
            template.removeAttr("index")
            template.children()
                .filter { it.localName == "Initialization" || it.localName == "RepresentationIndex" }
                .forEach(Element::remove)
            if (initializationName != null) {
                template.attr("initialization", initializationName)
            } else {
                template.removeAttr("initialization")
            }
            template.attr("media", "$key-\$Number\$.$segmentExtension")
            // FFmpeg numbers a template without one from 0, not 1, and would ask mpv for a file that isn't there.
            segments.media.firstOrNull()?.let { template.attr("startNumber", it.number.toString()) }
            LocalAddressing(fileName = null, element = template)
        }
        is DashAddressing.SegmentList -> {
            val list = addressing.segmentList.clone()
            list.childrenNamed("RepresentationIndex").forEach(Element::remove)
            list.childrenNamed("Initialization").forEach { initialization ->
                initialization.removeAttr("range")
                initialization.attr("sourceURL", checkNotNull(initializationName))
            }
            list.childrenNamed("SegmentURL").zip(segments.media).forEach { (element, segment) ->
                val name = "$key-${segment.number}.$segmentExtension"
                files += LocalDashFile(segment.segment, name)
                element.attributes().asList().forEach { element.removeAttr(it.key) }
                element.attr("media", name)
            }
            LocalAddressing(fileName = null, element = list)
        }
    }
}

private fun Element.removeAddressing() {
    children().filter { it.localName in ADDRESSING_ELEMENTS }.forEach { it.removeWithIndent() }
}

/** Removes the element with the line break and indent before it, so the written manifest has no blank lines. */
private fun Element.removeWithIndent() {
    (previousSibling() as? TextNode)?.takeIf(TextNode::isBlank)?.remove()
    remove()
}

private fun fileExtension(mimeType: String?): String {
    val mime = mimeType.orEmpty().lowercase()
    return when {
        "webm" in mime -> "webm"
        mime == "text/vtt" -> "vtt"
        "ttml" in mime -> "ttml"
        "mp2t" in mime -> "ts"
        else -> "mp4"
    }
}

// Where a player would fetch from or refresh the manifest.
private val NETWORK_ELEMENTS = setOf("BaseURL", "Location", "PatchLocation")
