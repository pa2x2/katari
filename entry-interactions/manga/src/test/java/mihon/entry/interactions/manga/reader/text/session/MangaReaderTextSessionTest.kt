package mihon.entry.interactions.manga.reader.text.session

import android.graphics.Color
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.manga.reader.text.translation.MangaPageTranslator
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.image.ImageContentKey
import mihon.translation.ui.session.language.TranslationLanguageContext
import org.junit.jupiter.api.Test

class MangaReaderTextSessionTest {

    private val recognition = FakeTextRecognition()
    private val pageLanguage = MutableStateFlow<LanguageTag?>(null)
    private val declaredLanguage = MutableStateFlow<LanguageTag?>(JAPANESE)

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

    private fun TestScope.session(): MangaReaderTextSession {
        val languages = TranslationLanguageContext(defaultTarget = { null }, store = null, scope = backgroundScope)
        return MangaReaderTextSession(
            recognition = recognition,
            modelStore = FakeModelStore(),
            translator = MangaPageTranslator(FakeTranslation(), languages, engineName = { null }),
            installPlatformModels = { _, _ -> TextRecognitionPlatformModelsResult.Installed },
            scope = backgroundScope,
            declaredLanguage = declaredLanguage,
            pageLanguage = pageLanguage,
            sampleBackground = { _, _ -> Color.WHITE },
        )
    }
}
