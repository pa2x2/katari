package mihon.entry.interactions.manga.reader.text.stored

import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationSetup
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.TextRecognitionResult

/**
 * A displayed page drawn from a stored translation: the stored regions placed on the displayed image, and the stored
 * translation of each region keyed by its displayed bounds. Regions without one are translated live.
 */
internal data class MangaStoredPage(
    val result: TextRecognitionResult,
    val translations: Map<ImageRect, String>,
    val setup: MangaChapterTranslationSetup,
)

/**
 * The stored page [image] shows, or `null` when no stored page was made from exactly its raw file: regions of a page
 * whose content changed would be drawn in the wrong places.
 */
internal fun MangaChapterTranslation.pageShownBy(image: DisplayedPageImage): MangaStoredPage? {
    val page = pages.firstOrNull { it.content == image.rawContent } ?: return null
    val placed = page.regions.mapNotNull { stored ->
        image.geometry.toDisplayed(stored.region)?.let { it to stored.translation }
    }
    return MangaStoredPage(
        result = TextRecognitionResult(image.key, image.size, setup.pageLanguage, placed.map { it.first }),
        translations = placed.mapNotNull { (region, translation) -> translation?.let { region.bounds to it } }.toMap(),
        setup = setup,
    )
}
