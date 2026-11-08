package app.katari.tts.provider.speechdispatcher.protocol

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SsipBlockAssemblerTest {
    @Test
    fun `a reply closes with a space after its code and keeps the data lines before it`() {
        val assembler = SsipBlockAssembler()

        assembler.accept("225-17") shouldBe null
        assembler.accept("225 OK MESSAGE QUEUED") shouldBe SsipReply(225, listOf("17"), "OK MESSAGE QUEUED")
    }

    @Test
    fun `an event between replies carries the id of the message it is about`() {
        val assembler = SsipBlockAssembler()

        listOf("702-17", "702-4").forEach { assembler.accept(it) shouldBe null }
        assembler.accept("702 END") shouldBe SsipEvent(17, SsipEventKind.End)
        assembler.accept("409 ERR RATE TOO HIGH") shouldBe SsipReply(409, emptyList(), "ERR RATE TOO HIGH")
    }
}
