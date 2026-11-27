package tachiyomi.domain.source.lnreader

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import java.time.ZoneOffset

class LnReaderReleaseTimeTest {

    @Test
    fun `each form a plugin sends reads as the same time, and text that is no date stays text`() {
        fun read(text: String) = lnReaderReleaseTime(JsonPrimitive(text), ZoneOffset.UTC)?.let { it.date to it.text }

        read("2024-03-03T10:20:00+02:00") shouldBe (1709454000000L to null)
        read("2024-03-03T08:20:00Z") shouldBe (1709454000000L to null)
        read("2024-03-03T08:20:00.000+0000") shouldBe (1709454000000L to null)
        read("2024-03-03 08:20:00") shouldBe (1709454000000L to null)
        read("2024-03-03") shouldBe (1709424000000L to null)
        read("Sunday, March 3, 2024 8:20 AM") shouldBe (1709454000000L to null)
        read("March 3, 2024 8:20 AM") shouldBe (1709454000000L to null)
        read("march 3, 2024") shouldBe (1709424000000L to null)
        read("Mar 3, 2024") shouldBe (1709424000000L to null)
        read("3 March 2024") shouldBe (1709424000000L to null)
        read("Sun Mar 3 2024") shouldBe (1709424000000L to null)
        read("2 days ago") shouldBe (0L to "2 days ago")
        read("LLL") shouldBe null
    }
}
