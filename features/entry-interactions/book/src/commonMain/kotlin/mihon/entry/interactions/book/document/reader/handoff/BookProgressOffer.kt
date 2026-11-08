package mihon.entry.interactions.book.document.reader.handoff

import mihon.book.api.BookLocator
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.interactions.media.handoff.EntryProgressOffer
import mihon.entry.interactions.media.handoff.ReadingPlace
import mihon.entry.interactions.media.handoff.isWorthOffering
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.math.roundToLong

/** Where another device got to, offered while reading (D053): [chapter], [percent] of the way through. */
internal data class BookProgressOffer(
    val deviceName: String?,
    val chapter: EntryChapter,
    /** Null when the other device's place can't be read here; going there then opens the chapter where it saved. */
    val locator: BookLocator?,
    val percent: Int,
)

/**
 * [offer] as a place among [chapters], in reading order, when it is worth offering to a reader [progression] of the way
 * through [currentChapterId]: a later chapter, or at least [MINIMUM_STEP_PERMILLE] further through the same one.
 */
internal fun bookProgressOffer(
    offer: EntryProgressOffer,
    chapters: List<EntryChapter>,
    currentChapterId: Long,
    progression: Double?,
): BookProgressOffer? {
    val offeredIndex = chapters.indexOfFirst { it.id == offer.progress.chapterId }
    val currentIndex = chapters.indexOfFirst { it.id == currentChapterId }
    if (offeredIndex < 0 || currentIndex < 0) return null
    val offered = offer.progress.locator.let { it.progression ?: it.totalProgression } ?: 0.0
    val here = ReadingPlace(currentIndex, (progression ?: 0.0).permille())
    if (!isWorthOffering(here, ReadingPlace(offeredIndex, offered.permille()), MINIMUM_STEP_PERMILLE)) return null
    return BookProgressOffer(
        deviceName = offer.deviceName,
        chapter = chapters[offeredIndex],
        locator = BookProgressLocatorCodec.decode(offer.progress.locator),
        percent = (offered * PERCENT).toInt(),
    )
}

private fun Double.permille(): Long = (this * PERMILLE).roundToLong()

private const val PERCENT = 100
private const val PERMILLE = 1000

// About a page of a chapter of ordinary length, so turning back a page or two offers nothing
private const val MINIMUM_STEP_PERMILLE = 20L
