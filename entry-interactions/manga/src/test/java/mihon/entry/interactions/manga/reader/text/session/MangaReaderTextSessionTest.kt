package mihon.entry.interactions.manga.reader.text.session

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.reader.text.surface.MangaPageTextDecoration
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import org.junit.jupiter.api.Test

class MangaReaderTextSessionTest {

    private val recognition = FakeTextRecognition()
    private val store = FakeModelStore()
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
        val session = session(declaredLanguage = null)
        session.onVisibleSurfaces(listOf(FakeSurface(0)))
        session.setActive(true)
        runCurrent()
        session.state.value.blocker shouldBe MangaReaderTextBlocker.LanguageRequired(listOf(JAPANESE))

        session.chooseLanguage(JAPANESE)
        runCurrent()

        session.state.value.progress.shouldBeInstanceOf<MangaReaderTextProgress.NoText>()
    }

    private fun TestScope.session(declaredLanguage: LanguageTag? = JAPANESE) =
        MangaReaderTextSession(
            recognition = recognition,
            modelStore = store,
            scope = backgroundScope,
            declaredLanguage = { declaredLanguage },
        )
}
