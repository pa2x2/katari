package app.katari.entry.interactions.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlaybackSpeedTest {
    @Test
    fun `a speed reads with its hundredths and is typed back onto a step within the range`() {
        // A fraction under ten hundredths needs its leading zero: 1.05x, not 1.5x
        PlaybackSpeed(105).label shouldBe "1.05x"
        PlaybackSpeed(120).label shouldBe "1.2x"
        PlaybackSpeed(400).label shouldBe "4x"

        PlaybackSpeed.parse("1,17x") shouldBe PlaybackSpeed(115)
        PlaybackSpeed.parse(" 0.25 ") shouldBe PlaybackSpeed(25)
        PlaybackSpeed.parse("4.5") shouldBe null
        PlaybackSpeed.parse("-1") shouldBe null
        PlaybackSpeed.parse("1.2.3") shouldBe null
    }
}
