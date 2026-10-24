package mihon.entry.interactions.anime.download.stream

import mihon.entry.interactions.anime.stream.PreferredHeight
import mihon.entry.interactions.anime.stream.rankByHeight
import mihon.video.engine.dash.DashAdaptationSet
import mihon.video.engine.dash.DashContentType
import mihon.video.engine.dash.DashManifest
import mihon.video.engine.dash.DashRepresentation
import mihon.video.engine.dash.UnsupportedDashManifestException

/**
 * The representations a DASH download keeps, each with the key its files are named after. In every period: the video
 * [preferredHeight] picks, one audio per language (and label or role, so a commentary track stays beside the main
 * one), and every subtitle set. Media [isDecodable] rejects is passed over while anything else is left, since the
 * built-in player couldn't play it; encrypted media always is, and a period with nothing else is refused.
 */
internal fun chooseDashRepresentations(
    manifest: DashManifest,
    preferredHeight: PreferredHeight,
    isDecodable: (DashRepresentation) -> Boolean,
): Map<DashRepresentation, String> {
    val kept = LinkedHashMap<DashRepresentation, String>()
    val keys = mutableMapOf<String, Int>()
    fun keep(representation: DashRepresentation, prefix: String) {
        kept[representation] = prefix + keys.merge(prefix, 1, Int::plus)
    }

    manifest.periods.forEach { period ->
        val sets = period.adaptationSets
        val videoSets = sets.filter { it.contentType == DashContentType.VIDEO && !it.isTrickPlay }
        val video = pickVideo(videoSets.clearRepresentations(), preferredHeight, isDecodable)
        if (video == null && videoSets.any { it.representations.isNotEmpty() }) throw copyProtected()
        video?.let { keep(it, "v") }

        val audioSets = sets.filter { it.contentType == DashContentType.AUDIO }
        val audio = audioSets.groupBy(DashAdaptationSet::audioGroup).values
            .mapNotNull { group -> pickAudio(group.clearRepresentations(), preferredHeight, isDecodable) }
        if (audio.isEmpty() && audioSets.any { it.representations.isNotEmpty() }) throw copyProtected()
        audio.forEach { keep(it, "a") }

        sets.filter { it.contentType == DashContentType.TEXT }
            .forEach { set -> set.representations.firstOrNull { !it.isProtected }?.let { keep(it, "t") } }
    }
    if (kept.isEmpty()) throw UnsupportedDashManifestException("No video or audio to download")
    return kept
}

private fun List<DashAdaptationSet>.clearRepresentations(): List<DashRepresentation> =
    flatMap { set -> set.representations.filterNot { it.isProtected } }

private fun pickVideo(
    representations: List<DashRepresentation>,
    preferredHeight: PreferredHeight,
    isDecodable: (DashRepresentation) -> Boolean,
): DashRepresentation? {
    val byBandwidth = representations.filter(isDecodable).ifEmpty {
        representations
    }.byPreferredBandwidth(preferredHeight)
    return rankByHeight(byBandwidth, preferredHeight) { it.height }.firstOrNull() ?: byBandwidth.firstOrNull()
}

private fun pickAudio(
    representations: List<DashRepresentation>,
    preferredHeight: PreferredHeight,
    isDecodable: (DashRepresentation) -> Boolean,
): DashRepresentation? =
    representations.filter(isDecodable).ifEmpty { representations }.byPreferredBandwidth(preferredHeight).firstOrNull()

/** Highest bandwidth first, or lowest first when the quality setting saves data. */
private fun List<DashRepresentation>.byPreferredBandwidth(preferredHeight: PreferredHeight) =
    if (preferredHeight == PreferredHeight.Lowest) sortedBy { it.bandwidth } else sortedByDescending { it.bandwidth }

private data class AudioGroup(val language: String?, val label: String?, val roles: Set<String>)

private fun DashAdaptationSet.audioGroup() =
    AudioGroup(language?.lowercase(), label, roles.filterNot { it == "main" }.toSet())

private fun copyProtected() = UndownloadableStreamException(UndownloadableStreamException.Reason.COPY_PROTECTED)
