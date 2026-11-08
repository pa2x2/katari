package mihon.tts.provider.speechdispatcher.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.nio.file.Path

class SpeechDispatcherAddressTest {
    @Test
    fun `the socket lives in the runtime directory`() {
        speechDispatcherAddress(mapOf("XDG_RUNTIME_DIR" to "/run/user/1000"), HOME) shouldBe
            SpeechDispatcherAddress.UnixSocket(Path.of("/run/user/1000/speech-dispatcher/speechd.sock"))
    }

    @Test
    fun `without a runtime directory the socket lives in the cache directory`() {
        speechDispatcherAddress(emptyMap(), HOME) shouldBe
            SpeechDispatcherAddress.UnixSocket(Path.of("/home/reader/.cache/speech-dispatcher/speechd.sock"))
    }

    @Test
    fun `SPEECHD_ADDRESS names another socket or a network port`() {
        speechDispatcherAddress(mapOf("SPEECHD_ADDRESS" to "unix_socket:/tmp/speech.sock"), HOME) shouldBe
            SpeechDispatcherAddress.UnixSocket(Path.of("/tmp/speech.sock"))
        speechDispatcherAddress(mapOf("SPEECHD_ADDRESS" to "inet_socket:speech.local:7000"), HOME) shouldBe
            SpeechDispatcherAddress.InetSocket("speech.local", 7000)
    }

    private companion object {
        const val HOME = "/home/reader"
    }
}
