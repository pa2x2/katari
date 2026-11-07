package eu.kanade.tachiyomi.ui.reader.handoff

import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.media.handoff.EntryProgressOffer
import mihon.entry.interactions.media.handoff.ReadingPlace
import mihon.entry.interactions.media.handoff.isWorthOffering

/** Where another device got to, offered while reading (D053): [chapterName], at the page of [pageIndex]. */
internal data class ReaderProgressOffer(
    val deviceName: String?,
    val chapterId: Long,
    val chapterName: String,
    val pageIndex: Int,
)

/**
 * [offer] as a place among [chapters], in reading order, when it is worth offering to a reader at [currentPageIndex]
 * of [current]: a later chapter, or at least [MINIMUM_PAGES] pages on in the same one.
 */
internal fun readerProgressOffer(
    offer: EntryProgressOffer,
    chapters: List<ReaderChapter>,
    current: ReaderChapter?,
    currentPageIndex: Int,
): ReaderProgressOffer? {
    val chapterIndex = chapters.indexOfFirst { it.chapter.id == offer.progress.chapterId }
    val currentIndex = chapters.indexOf(current)
    if (chapterIndex < 0 || currentIndex < 0) return null
    val pageIndex = offer.progress.locator.position?.toInt() ?: 0
    val here = ReadingPlace(currentIndex, currentPageIndex.toLong())
    if (!isWorthOffering(here, ReadingPlace(chapterIndex, pageIndex.toLong()), MINIMUM_PAGES)) return null
    val chapter = chapters[chapterIndex].chapter
    return ReaderProgressOffer(offer.deviceName, checkNotNull(chapter.id), chapter.name, pageIndex)
}

// Never for a page or two, which the person may just have flipped back
private const val MINIMUM_PAGES = 3L
