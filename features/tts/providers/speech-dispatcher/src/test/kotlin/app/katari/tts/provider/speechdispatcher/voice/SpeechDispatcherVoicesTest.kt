package app.katari.tts.provider.speechdispatcher.voice

import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.engine.TtsProviderId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SpeechDispatcherVoicesTest {
    @Test
    fun `variants laid over every base voice are left out`() {
        val voices = parseSynthesisVoices(
            "espeak-ng",
            listOf(
                "English (Great Britain)\ten-GB\tnone",
                "English (Great Britain)+Alex\ten-GB\tAlex",
                "Japanese\tja\tnone",
            ),
        )

        voices.map(SpeechDispatcherVoice::name) shouldBe listOf("English (Great Britain)", "Japanese")
    }

    @Test
    fun `a module without base voices keeps all it lists`() {
        val voices = parseSynthesisVoices("piper", listOf("en_US-amy-medium\ten-US\tamy"))

        voices.map(SpeechDispatcherVoice::name) shouldBe listOf("en_US-amy-medium")
    }

    @Test
    fun `Mandarin voices match Chinese text`() {
        val voice = SpeechDispatcherVoice("espeak-ng", "Chinese (Mandarin, latin as Pinyin)", "cmn-LATN-PINYIN")

        voice.toApiVoice(PROVIDER, ENGINE, namesModule = false)?.language?.value shouldBe "zh-Latn-PINYIN"
    }

    private companion object {
        val PROVIDER = TtsProviderId("speech-dispatcher")
        val ENGINE = TtsEngineId("speech-dispatcher")
    }
}
