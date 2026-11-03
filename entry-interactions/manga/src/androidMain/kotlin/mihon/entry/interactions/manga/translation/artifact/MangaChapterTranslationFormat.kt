package mihon.entry.interactions.manga.translation.artifact

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.request.ResolvedTranslationRoute

/**
 * The stored file's layout. Files outlive app versions, so the layout is versioned and its names are fixed here rather
 * than taken from API types; a later layout gets a new version and this one stays readable.
 */
internal object MangaChapterTranslationFormat {
    private const val VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(translation: MangaChapterTranslation): String = json.encodeToString(translation.toV1())

    /** The stored translation, or `null` when [text] is of another version or not a readable file. */
    fun decode(text: String): MangaChapterTranslation? = try {
        if (json.decodeFromString<Header>(text).format != VERSION) {
            null
        } else {
            json.decodeFromString<ChapterV1>(text).toModel()
        }
    } catch (_: IllegalArgumentException) {
        // Malformed JSON (a SerializationException) or values no longer valid, such as an unknown language.
        null
    }

    @Serializable
    private class Header(val format: Int)

    @Serializable
    private class ChapterV1(
        val format: Int,
        val setup: SetupV1,
        val pages: List<PageV1>,
    )

    @Serializable
    private class SetupV1(
        val pageLanguage: String,
        val detector: String,
        val recognizer: String,
        val engine: String,
        val source: String,
        val target: String,
    )

    @Serializable
    private class PageV1(
        val file: String,
        val sha256: String,
        val width: Int,
        val height: Int,
        val regions: List<RegionV1>,
    )

    /** Rectangles are `[left, top, right, bottom]` in raw-page pixels. */
    @Serializable
    private class RegionV1(
        val bounds: List<Int>,
        val container: List<Int>? = null,
        val kind: KindV1,
        val orientation: OrientationV1,
        val text: String,
        val translation: String? = null,
    )

    @Serializable
    private enum class KindV1 {
        @SerialName("bubble")
        Bubble,

        @SerialName("free")
        Free,

        @SerialName("unclassified")
        Unclassified,
    }

    @Serializable
    private enum class OrientationV1 {
        @SerialName("horizontal")
        Horizontal,

        @SerialName("vertical")
        Vertical,

        @SerialName("unknown")
        Unknown,
    }

    private fun MangaChapterTranslation.toV1() = ChapterV1(
        format = VERSION,
        setup = SetupV1(
            pageLanguage = setup.pageLanguage.value,
            detector = setup.pipeline.detector.value,
            recognizer = setup.pipeline.recognizer.value,
            engine = setup.route.engine.value,
            source = setup.route.sourceLanguage.value,
            target = setup.route.targetLanguage.value,
        ),
        pages = pages.map { page ->
            PageV1(
                file = page.fileName,
                sha256 = page.content.value,
                width = page.size.width,
                height = page.size.height,
                regions = page.regions.map { (region, translation) ->
                    RegionV1(
                        bounds = region.bounds.toV1(),
                        container = region.container?.toV1(),
                        kind = when (region.kind) {
                            TextRegionKind.SpeechBubble -> KindV1.Bubble
                            TextRegionKind.FreeText -> KindV1.Free
                            TextRegionKind.Unclassified -> KindV1.Unclassified
                        },
                        orientation = when (region.orientation) {
                            TextOrientation.Horizontal -> OrientationV1.Horizontal
                            TextOrientation.Vertical -> OrientationV1.Vertical
                            TextOrientation.Unknown -> OrientationV1.Unknown
                        },
                        text = region.text,
                        translation = translation,
                    )
                },
            )
        },
    )

    private fun ChapterV1.toModel() = MangaChapterTranslation(
        setup = MangaChapterTranslationSetup(
            pageLanguage = LanguageTag.require(setup.pageLanguage),
            pipeline = TextRecognitionPipeline(
                detector = TextRecognitionComponentId(setup.detector),
                recognizer = TextRecognitionComponentId(setup.recognizer),
            ),
            route = ResolvedTranslationRoute(
                sourceLanguage = LanguageTag.require(setup.source),
                targetLanguage = LanguageTag.require(setup.target),
                engine = TranslationEngineId(setup.engine),
            ),
        ),
        pages = pages.map { page ->
            MangaTranslatedPage(
                fileName = page.file,
                content = ImageContentKey(page.sha256),
                size = ImageSize(page.width, page.height),
                regions = page.regions.map { region ->
                    MangaTranslatedRegion(
                        region = RecognizedTextRegion(
                            bounds = region.bounds.toRect(),
                            text = region.text,
                            kind = when (region.kind) {
                                KindV1.Bubble -> TextRegionKind.SpeechBubble
                                KindV1.Free -> TextRegionKind.FreeText
                                KindV1.Unclassified -> TextRegionKind.Unclassified
                            },
                            orientation = when (region.orientation) {
                                OrientationV1.Horizontal -> TextOrientation.Horizontal
                                OrientationV1.Vertical -> TextOrientation.Vertical
                                OrientationV1.Unknown -> TextOrientation.Unknown
                            },
                            container = region.container?.toRect(),
                        ),
                        translation = region.translation,
                    )
                },
            )
        },
    )

    private fun ImageRect.toV1() = listOf(left, top, right, bottom)

    private fun List<Int>.toRect(): ImageRect {
        require(size == 4) { "A rectangle has four edges" }
        return ImageRect(this[0], this[1], this[2], this[3])
    }
}
