package app.katari.entry.interactions.manga.translation.artifact

import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.result.RecognizedTextRegion
import app.katari.translation.api.request.ResolvedTranslationRoute

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
