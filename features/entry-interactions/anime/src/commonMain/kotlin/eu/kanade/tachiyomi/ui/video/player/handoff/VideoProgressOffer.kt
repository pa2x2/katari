package eu.kanade.tachiyomi.ui.video.player.handoff

import mihon.entry.interactions.media.handoff.EntryProgressOffer
import mihon.entry.interactions.media.handoff.ReadingPlace
import mihon.entry.interactions.media.handoff.isWorthOffering
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.time.Duration.Companion.minutes

/** Where another device got to, offered while watching (D053): [episodeName], at [positionMs]. */
internal data class VideoProgressOffer(
    val deviceName: String?,
    val ownerEntryId: Long,
    val episodeId: Long,
    val episodeName: String,
    val positionMs: Long,
)

/**
 * [offer] as a place among [episodes], in watching order, when it is worth offering to a viewer at [positionMs] of
 * [episodeId]: a later episode, or at least a minute on in the same one.
 */
internal fun videoProgressOffer(
    offer: EntryProgressOffer,
    episodes: List<EntryChapter>,
    episodeId: Long,
    positionMs: Long,
): VideoProgressOffer? {
    val offeredIndex = episodes.indexOfFirst { it.id == offer.progress.chapterId }
    val currentIndex = episodes.indexOfFirst { it.id == episodeId }
    if (offeredIndex < 0 || currentIndex < 0) return null
    val offeredMs = offer.progress.locator.position ?: 0L
    val here = ReadingPlace(currentIndex, positionMs)
    if (!isWorthOffering(here, ReadingPlace(offeredIndex, offeredMs), MINIMUM_STEP_MS)) return null
    val episode = episodes[offeredIndex]
    return VideoProgressOffer(offer.deviceName, episode.entryId, episode.id, episode.name, offeredMs)
}

private val MINIMUM_STEP_MS = 1.minutes.inWholeMilliseconds
