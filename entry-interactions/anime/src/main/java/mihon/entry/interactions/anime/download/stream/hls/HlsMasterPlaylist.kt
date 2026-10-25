package mihon.entry.interactions.anime.download.stream.hls

import okhttp3.HttpUrl

/** A multivariant playlist's variants and renditions, read from its [lines]; references resolve against [url]. */
internal class HlsMasterPlaylist(private val lines: List<String>, private val url: HttpUrl) {
    val variants: List<HlsVariant>
    val renditions: List<HlsRendition>

    init {
        val variants = ArrayList<HlsVariant>()
        val renditions = ArrayList<HlsRendition>()
        var pendingVariant: Pair<Int, HlsTag>? = null
        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEachIndexed
            val tag = HlsTag.parse(line)
            when {
                tag?.name == STREAM_INF -> pendingVariant = index to tag
                tag?.name == MEDIA -> renditions += HlsRendition(index, tag, tag["URI"]?.let(::resolve))
                tag == null && !line.startsWith("#") -> pendingVariant?.let { (tagLine, variantTag) ->
                    resolve(line)?.let { variants += HlsVariant(tagLine, index, variantTag, it) }
                    pendingVariant = null
                }
            }
        }
        this.variants = variants
        this.renditions = renditions
    }

    /**
     * The playlist with only [variant] and the [renditions] kept, named [variantName] and what [nameOf] gives each.
     * Trick-play and image playlists go, and so does every other tag that points at the network, so the player has
     * nothing to fetch; a video rendition group is dropped with its renditions, the variant playing its own playlist.
     */
    fun local(
        variant: HlsVariant,
        variantName: String,
        renditions: List<HlsRendition>,
        nameOf: (HlsRendition) -> String,
    ): String {
        val kept = renditions.associateBy(HlsRendition::line)
        return buildList {
            lines.forEachIndexed { index, rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEachIndexed
                val tag = HlsTag.parse(line)
                when {
                    tag == null && line.startsWith("#") -> add(line)
                    tag == null -> if (index == variant.uriLine) add(variantName)
                    tag.name == STREAM_INF -> if (index == variant.tagLine) add(tag.with("VIDEO" to null))
                    tag.name == MEDIA -> kept[index]?.let { rendition ->
                        add(if (rendition.url == null) line else tag.with("URI" to nameOf(rendition)))
                    }
                    tag["URI"] == null && tag.name !in NETWORK_TAGS -> add(line)
                }
            }
        }.joinToString("\n", postfix = "\n")
    }

    private fun resolve(reference: String): HttpUrl? = url.resolve(reference)
}

/** An `EXT-X-STREAM-INF` variant: its tag on line [tagLine], its playlist's [url] on line [uriLine]. */
internal class HlsVariant(val tagLine: Int, val uriLine: Int, tag: HlsTag, val url: HttpUrl) {
    val bandwidth: Long = tag["BANDWIDTH"]?.toLongOrNull() ?: 0L
    val height: Int? = tag["RESOLUTION"]?.substringAfter('x', "")?.toIntOrNull()
    val codecs: String? = tag["CODECS"]
    val audioGroup: String? = tag["AUDIO"]
    val subtitlesGroup: String? = tag["SUBTITLES"]
    val closedCaptionsGroup: String? = tag["CLOSED-CAPTIONS"]
}

/** An `EXT-X-MEDIA` rendition on [line]; [url] is null for one carried in the variant's own segments. */
internal class HlsRendition(val line: Int, tag: HlsTag, val url: HttpUrl?) {
    val type: String? = tag["TYPE"]
    val groupId: String? = tag["GROUP-ID"]
}

/** Whether [text] is a multivariant playlist rather than a media playlist. */
internal fun isHlsMasterPlaylist(text: String): Boolean = text.lineSequence().any { it.trim().startsWith(STREAM_INF) }

/** Whether [text] is an HLS playlist at all. */
internal fun isHlsPlaylist(
    text: String,
): Boolean = text.trimStart('\uFEFF', ' ', '\t', '\r', '\n').startsWith("#EXTM3U")

private const val STREAM_INF = "#EXT-X-STREAM-INF"
private const val MEDIA = "#EXT-X-MEDIA"

// Tags whose reference isn't in a URI attribute, or that a downloaded copy has no use for.
private val NETWORK_TAGS = setOf("#EXT-X-CONTENT-STEERING", "#EXT-X-SESSION-KEY")
