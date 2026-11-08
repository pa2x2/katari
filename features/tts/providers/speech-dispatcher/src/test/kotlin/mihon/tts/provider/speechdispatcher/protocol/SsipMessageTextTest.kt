package mihon.tts.provider.speechdispatcher.protocol

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SsipMessageTextTest {
    @Test
    fun `lines starting with a dot get a second one so they can't end the message early`() {
        ssipMessageBody("First\n.\r\n..two\rlast") shouldBe "First\r\n..\r\n...two\r\nlast\r\n.\r\n"
    }
}
