package mihon.tts.ui.settings

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.tts.api.request.TtsLanguageSelection
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPreviewControllerTest {

    @Test
    fun `preview speaks only the fixed language sample and configuration changes stop it`() = runTest {
        val host = TestTtsHostActions()
        val feature = TestTtsFeature()
        val controller = TtsSettingsController(feature, host, backgroundScope, ENGLISH)
        runCurrent()

        controller.togglePreview()
        runCurrent()

        feature.preparedRequests.single() shouldBe mihon.tts.api.request.TtsRequest(
            text = previewSample(ENGLISH).text,
            language = TtsLanguageSelection.Explicit(ENGLISH),
        )
        controller.state.value.preview shouldBe TtsPreviewState.Speaking

        controller.setSpeechRate(1.25f)
        runCurrent()

        host.speechRate.get() shouldBe 1.25f
        feature.session.stopCount shouldBe 1
        controller.state.value.preview shouldBe TtsPreviewState.Idle
        controller.close()
    }
}
