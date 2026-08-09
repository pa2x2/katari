package mihon.tts.ui.settings

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import java.util.Locale

class TtsVoiceCatalogTest {

    @Test
    fun `compatible voices include language variants and prefer on-device processing`() {
        TEST_VOICES.compatibleWith(PORTUGUESE_BRAZIL) shouldContainExactly listOf(
            PORTUGUESE_LOCAL_VOICE,
            PORTUGUESE_NETWORK_VOICE,
        )
    }

    @Test
    fun `language options reflect network policy and omit existing override contexts`() {
        ttsLanguageOptions(
            voices = TEST_VOICES,
            allowNetworkVoices = false,
            excludedLanguages = setOf(ENGLISH),
            locale = Locale.ENGLISH,
        ).map(TtsLanguageOption::tag) shouldContainExactly listOf(PORTUGUESE_PORTUGAL)
    }
}
