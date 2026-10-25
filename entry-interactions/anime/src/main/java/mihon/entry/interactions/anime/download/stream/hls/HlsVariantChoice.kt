package mihon.entry.interactions.anime.download.stream.hls

import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.entry.interactions.anime.stream.rankByHeight

/** The variant an HLS download keeps and the renditions it plays with. */
internal class HlsVariantChoice(val variant: HlsVariant, val renditions: List<HlsRendition>)

/**
 * What an HLS download keeps of [master], by the rule a DASH download follows: the variant [preferredHeight] picks
 * among those whose codecs [isDecodable] accepts (all of them when it accepts none), the highest bandwidth of a
 * height first, or the lowest when the setting saves data; then the audio, subtitle and closed-caption renditions of
 * that variant's groups.
 */
internal fun chooseHlsVariant(
    master: HlsMasterPlaylist,
    preferredHeight: PreferredHeight,
    isDecodable: (codecs: String?) -> Boolean,
): HlsVariantChoice {
    val variants = master.variants.ifEmpty { throw UnsupportedOperationException("Master playlist without variants") }
    val candidates = variants.filter { isDecodable(it.codecs) }.ifEmpty { variants }
    val byBandwidth = if (preferredHeight == PreferredHeight.Lowest) {
        candidates.sortedBy(HlsVariant::bandwidth)
    } else {
        candidates.sortedByDescending(HlsVariant::bandwidth)
    }
    val variant = rankByHeight(byBandwidth, preferredHeight, HlsVariant::height).firstOrNull() ?: byBandwidth.first()
    val renditions = master.renditions.filter { rendition ->
        rendition.groupId != null && when (rendition.type) {
            "AUDIO" -> rendition.groupId == variant.audioGroup
            "SUBTITLES" -> rendition.groupId == variant.subtitlesGroup
            "CLOSED-CAPTIONS" -> rendition.groupId == variant.closedCaptionsGroup
            else -> false
        }
    }
    return HlsVariantChoice(variant, renditions)
}
