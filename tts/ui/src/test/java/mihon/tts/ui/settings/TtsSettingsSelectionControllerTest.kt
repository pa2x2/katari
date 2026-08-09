package mihon.tts.ui.settings

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mihon.tts.api.voice.TtsVoiceId
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsSettingsSelectionControllerTest {

    @Test
    fun `only a ready engine can replace the explicit profile selection`() = runTest {
        val host = TestTtsHostActions()
        val controller = TtsSettingsController(TestTtsFeature(), host, backgroundScope, ENGLISH)
        runCurrent()

        controller.selectEngine(BLOCKED_ENGINE)
        host.selectedEngine.get() shouldBe FIRST_ENGINE

        controller.selectEngine(SECOND_ENGINE)
        host.selectedEngine.get() shouldBe SECOND_ENGINE
        host.selectedEngine.isSet() shouldBe true
        controller.close()
    }

    @Test
    fun `voice overrides accept usable language variants and reject unavailable or prohibited voices`() = runTest {
        val host = TestTtsHostActions()
        val controller = TtsSettingsController(TestTtsFeature(), host, backgroundScope, ENGLISH)
        runCurrent()

        controller.setVoiceOverride(PORTUGUESE_BRAZIL, PORTUGUESE_LOCAL_VOICE.id)
        controller.setVoiceOverride(
            PORTUGUESE_BRAZIL,
            TtsVoiceId(PROVIDER, FIRST_ENGINE, "not-installed"),
        )
        controller.setVoiceOverride(PORTUGUESE_BRAZIL, PORTUGUESE_NETWORK_VOICE.id)

        host.selectedVoiceOverrides() shouldContainExactly mapOf(
            PORTUGUESE_BRAZIL to PORTUGUESE_LOCAL_VOICE.id,
        )

        controller.setVoiceOverride(PORTUGUESE_BRAZIL, null)
        host.selectedVoiceOverrides() shouldBe emptyMap()
        controller.close()
    }

    @Test
    fun `provider return re-inspects voices for the unchanged selected engine`() = runTest {
        val host = TestTtsHostActions()
        val controller = TtsSettingsController(TestTtsFeature(), host, backgroundScope, ENGLISH)
        runCurrent()
        host.voiceInspectionCount shouldBe 1

        host.voices = listOf(ENGLISH_VOICE)
        controller.refresh(forceVoiceRefresh = true)
        runCurrent()

        host.voiceInspectionCount shouldBe 2
        controller.state.value.voiceCatalog shouldBe TtsVoiceCatalogState.Available(
            engine = FIRST_ENGINE,
            voices = listOf(ENGLISH_VOICE),
        )
        controller.close()
    }
}
