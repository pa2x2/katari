package mihon.translation.ui.session.language

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.language.api.identification.TextLanguageResolutionContext
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationDefaultTarget
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationLanguageContextTest {
    @Test
    fun `choices are kept and apply to later requests before the store reports them`() = runTest {
        val store = RecordingStore()
        val context = context(store)
        runCurrent()

        context.selectSource(JAPANESE) shouldBe TranslationSourceLanguageSelection.Explicit(JAPANESE)
        context.selectTarget(SPANISH) shouldBe TranslationTargetLanguageSelection.Explicit(SPANISH)

        with(context.request("next", TextLanguageResolutionContext())) {
            sourceLanguage shouldBe TranslationSourceLanguageSelection.Explicit(JAPANESE)
            targetLanguage shouldBe TranslationTargetLanguageSelection.Explicit(SPANISH)
        }
        runCurrent()
        store.sources shouldBe listOf(JAPANESE)
        store.targets shouldBe listOf(SPANISH)
    }

    @Test
    fun `choosing the language the profile target resolves to follows the profile`() = runTest {
        val store = RecordingStore(TranslationStoredLanguages(target = SPANISH))
        val context = context(store)
        runCurrent()

        context.selectTarget(ENGLISH) shouldBe TranslationTargetLanguageSelection.Default
        runCurrent()

        store.targets shouldBe listOf(null)
        context.request("text", TextLanguageResolutionContext()).targetLanguage shouldBe
            TranslationTargetLanguageSelection.Default
    }

    private fun TestScope.context(store: TranslationLanguageStore?) = TranslationLanguageContext(
        defaultTarget = { TranslationDefaultTarget(ENGLISH, followsAppLanguage = true) },
        store = store,
        scope = backgroundScope,
    )

    private class RecordingStore(initial: TranslationStoredLanguages = TranslationStoredLanguages()) :
        TranslationLanguageStore {
        override val languages = MutableStateFlow(initial)
        val sources = mutableListOf<LanguageTag?>()
        val targets = mutableListOf<LanguageTag?>()

        override suspend fun setSourceLanguage(language: LanguageTag?) {
            sources += language
            languages.value = languages.value.copy(source = language)
        }

        override suspend fun setTargetLanguage(language: LanguageTag?) {
            targets += language
            languages.value = languages.value.copy(target = language)
        }
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val JAPANESE = LanguageTag.require("ja")
        val SPANISH = LanguageTag.require("es")
    }
}
