package mihon.tts.ui.settings

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test

class TtsVoiceCatalogTest {

    @Test
    fun `compatible voices include language variants and prefer on-device processing`() {
        TEST_VOICES.compatibleWith(PORTUGUESE_BRAZIL) shouldContainExactly listOf(
            PORTUGUESE_LOCAL_VOICE,
            PORTUGUESE_NETWORK_VOICE,
        )
    }
}
