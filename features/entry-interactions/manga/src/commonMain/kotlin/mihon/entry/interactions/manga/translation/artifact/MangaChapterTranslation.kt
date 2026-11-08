package mihon.entry.interactions.manga.translation.artifact

import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.translation.api.request.ResolvedTranslationRoute

/**
 * The stored recognition and translation of a downloaded chapter. Pages it lacks, and regions without a translation,
 * are gaps the reader fills live.
 */
internal data class MangaChapterTranslation(
    val setup: MangaChapterTranslationSetup,
    val pages: List<MangaTranslatedPage>,
)

/** What produced a stored translation; shown to the user, never used to discard it. */
internal data class MangaChapterTranslationSetup(
    val pageLanguage: LanguageTag,
    val pipeline: TextRecognitionPipeline,
    val route: ResolvedTranslationRoute,
)

/**
 * One raw page file of the chapter, identified by [content] so a page whose bytes changed is not matched.
 * Regions are in raw-page pixels of [size].
 */
internal data class MangaTranslatedPage(
    val fileName: String,
    val content: ImageContentKey,
    val size: ImageSize,
    val regions: List<MangaTranslatedRegion>,
)

/** A recognized region and its translation, or `null` when translating it failed. */
internal data class MangaTranslatedRegion(
    val region: RecognizedTextRegion,
    val translation: String?,
)
