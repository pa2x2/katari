package mihon.entry.interactions.manga.reader.text.session

import android.graphics.Color
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslationIssue
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslator
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.ui.session.language.TranslationLanguageContext
import org.junit.jupiter.api.Test

class MangaReaderTextSessionTest {

    private val recognition = FakeTextRecognition()
    private val store = FakeModelStore()
    private val translation = FakeTranslation()
    private val pageLanguage = MutableStateFlow<LanguageTag?>(null)
    private val declaredLanguage = MutableStateFlow<LanguageTag?>(JAPANESE)
    private lateinit var languages: TranslationLanguageContext
    private val text = ImageRect(420, 140, 480, 330)
    private val container = ImageRect(380, 100, 520, 360)

    @Test
    fun `activating recognizes the visible pages and outlines their bubbles`() = runTest {
        recognition.regions = listOf(bubble("素直にあやまるしか", text, container))
        val surface = FakeSurface(0)
        val session = session()

        session.onVisibleSurfaces(listOf(surface))
        session.setActive(true)
        runCurrent()

        session.state.value.progress shouldBe MangaReaderTextProgress.Ready
        session.decoration(surface.page).first() shouldBe MangaPageTextDecoration(PAGE_SIZE, listOf(container))
    }

    @Test
    fun `a tap anywhere on a bubble finds the text it contains`() = runTest {
        recognition.regions = listOf(bubble("素直にあやまるしか", text, container))
        val surface = FakeSurface(0)
        val session = session()
        session.onVisibleSurfaces(listOf(surface))
        session.setActive(true)
        runCurrent()

        session.regionAt(surface.page, x = 390, y = 350)?.text shouldBe "素直にあやまるしか"
        session.regionAt(surface.page, x = 600, y = 350) shouldBe null
    }

    @Test
    fun `missing models block recognition until the approved download is installed`() = runTest {
        recognition.preparation = { TextRecognitionPreparation.ModelsRequired(JAPANESE, PIPELINE, listOf(MODEL)) }
        val session = session()
        session.onVisibleSurfaces(listOf(FakeSurface(0)))
        session.setActive(true)
        runCurrent()

        session.state.value.blocker shouldBe MangaReaderTextBlocker.ModelsRequired(listOf(MODEL))
        recognition.recognized shouldBe emptyList()

        val approval = ModelArtifactDownloadApproval(MODEL, allowMeteredNetwork = false)
        session.approveModels(listOf(approval))
        recognition.preparation = FakeTextRecognition().preparation
        store.install(MODEL)
        runCurrent()

        store.approved shouldContainExactly listOf(approval)
        session.state.value.blocker shouldBe null
        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-0"))
    }

    @Test
    fun `a page that leaves the screen before it is recognized is not reported`() = runTest {
        val release = CompletableDeferred<Unit>().also { recognition.release = it }
        val first = FakeSurface(0)
        val second = FakeSurface(1)
        val session = session()
        session.onVisibleSurfaces(listOf(first))
        session.setActive(true)
        runCurrent()
        session.state.value.progress shouldBe MangaReaderTextProgress.Recognizing

        session.onVisibleSurfaces(listOf(second))
        release.complete(Unit)
        runCurrent()

        session.state.value.pages.keys shouldContainExactly setOf(second.page)
        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-1"))
    }

    @Test
    fun `a multi-language source asks for the language and resumes once it is chosen`() = runTest {
        recognition.preparation = { request ->
            if (request.language == null) {
                TextRecognitionPreparation.LanguageRequired(listOf(JAPANESE))
            } else {
                FakeTextRecognition().preparation(request)
            }
        }
        declaredLanguage.value = null
        val session = session()
        session.onVisibleSurfaces(listOf(FakeSurface(0)))
        session.setActive(true)
        runCurrent()
        session.state.value.blocker shouldBe MangaReaderTextBlocker.LanguageRequired(listOf(JAPANESE))

        pageLanguage.value = JAPANESE
        runCurrent()

        session.state.value.progress.shouldBeInstanceOf<MangaReaderTextProgress.NoText>()
    }

    @Test
    fun `pages are read in the language kept for the series instead of the declared one`() = runTest {
        pageLanguage.value = KOREAN
        val session = session()
        session.onVisibleSurfaces(listOf(FakeSurface(0)))
        session.setActive(true)
        runCurrent()

        recognition.recognizedLanguages shouldContainExactly listOf(KOREAN)
        session.state.value.language shouldBe KOREAN
        session.state.value.languageKept shouldBe true
    }

    @Test
    fun `another page language reads recognized pages again in it`() = runTest {
        recognition.regions = listOf(bubble("素直にあやまるしか", text, container))
        val surface = FakeSurface(0)
        val session = session()
        session.onVisibleSurfaces(listOf(surface))
        session.setActive(true)
        runCurrent()

        pageLanguage.value = KOREAN
        runCurrent()

        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-0"), ImageContentKey("page-0"))
        recognition.recognizedLanguages shouldContainExactly listOf(JAPANESE, KOREAN)
        session.state.value.progress shouldBe MangaReaderTextProgress.Ready
    }

    @Test
    fun `the source's language counts once the series is loaded after the session started`() = runTest {
        declaredLanguage.value = null
        val session = session()
        runCurrent()

        declaredLanguage.value = JAPANESE
        runCurrent()

        session.state.value.declaredLanguage shouldBe JAPANESE
        session.state.value.language shouldBe JAPANESE
        session.state.value.languageKept shouldBe false
    }

    @Test
    fun `learning the source's language does not read pages again in the kept language`() = runTest {
        declaredLanguage.value = null
        pageLanguage.value = KOREAN
        val session = session()
        session.onVisibleSurfaces(listOf(FakeSurface(0)))
        session.setActive(true)
        runCurrent()

        declaredLanguage.value = JAPANESE
        runCurrent()

        session.state.value.declaredLanguage shouldBe JAPANESE
        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-0"))
    }

    @Test
    fun `drawn translations replace the outlines of the regions they translate`() = runTest {
        val narration = RecognizedTextRegion(
            bounds = ImageRect(100, 1500, 700, 1560),
            text = "語り",
            kind = TextRegionKind.FreeText,
            orientation = TextOrientation.Horizontal,
        )
        recognition.regions = listOf(bubble("素直に", text, container), narration)
        translation.preparation = null
        val surface = FakeSurface(0)
        val session = session()
        session.onVisibleSurfaces(listOf(surface))
        session.setOverlay(true)
        session.setActive(true)
        runCurrent()

        val decoration = session.decoration(surface.page).first()!!
        decoration.regions shouldBe emptyList()
        decoration.overlays.map { it.source to it.text } shouldContainExactly
            listOf(text to "素直に".uppercase(), narration.bounds to "語り")

        session.toggleOriginal()

        session.decoration(surface.page).first()!!.overlays shouldBe emptyList()
    }

    @Test
    fun `another target replaces translations drawn on the page`() = runTest {
        recognition.regions = listOf(bubble("素直に", text, container))
        val surface = FakeSurface(0)
        val session = session()
        session.onVisibleSurfaces(listOf(surface))
        session.setOverlay(true)
        session.setActive(true)
        runCurrent()

        languages.selectTarget(FRENCH)
        runCurrent()

        translation.requests.map { it.targetLanguage } shouldContainExactly listOf(
            TranslationTargetLanguageSelection.Default,
            TranslationTargetLanguageSelection.Explicit(FRENCH),
        )
        session.decoration(surface.page).first()!!.overlays.map { it.text } shouldContainExactly
            listOf("素直に".uppercase())
    }

    @Test
    fun `a missing target stops drawing translations but not tapping`() = runTest {
        recognition.regions = listOf(bubble("素直に", text, container))
        translation.preparation = TranslationPreparation.TargetLanguageRequired(
            sourceLanguage = JAPANESE,
            reason = TranslationTargetChoiceReason.NoDefaultTarget,
        )
        val surface = FakeSurface(0)
        val session = session()
        session.onVisibleSurfaces(listOf(surface))
        session.setOverlay(true)
        session.setActive(true)
        runCurrent()

        session.state.value.progress shouldBe
            MangaReaderTextProgress.TranslationUnavailable(MangaPageTranslationIssue.TargetRequired)
        session.regionAt(surface.page, x = 390, y = 350)?.text shouldBe "素直に"
    }

    @Test
    fun `preloaded pages are processed only while translations are drawn on pages`() = runTest {
        val visible = FakeSurface(0)
        val preloaded = FakeSurface(1)
        val session = session()
        session.setActive(true)

        session.onVisibleSurfaces(listOf(visible), preloaded = listOf(preloaded))
        runCurrent()
        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-0"))

        session.setOverlay(true)
        session.onVisibleSurfaces(listOf(visible), preloaded = listOf(preloaded))
        runCurrent()
        recognition.recognized shouldContainExactly listOf(ImageContentKey("page-0"), ImageContentKey("page-1"))
    }

    private fun TestScope.session(): MangaReaderTextSession {
        languages = TranslationLanguageContext(defaultTarget = { null }, store = null, scope = backgroundScope)
        return MangaReaderTextSession(
            recognition = recognition,
            modelStore = store,
            translator = MangaPageTranslator(translation, languages, engineName = { null }),
            installPlatformModels = { _, _ -> TextRecognitionPlatformModelsResult.Installed },
            scope = backgroundScope,
            declaredLanguage = declaredLanguage,
            pageLanguage = pageLanguage,
            sampleBackground = { _, _ -> Color.WHITE },
        )
    }
}
