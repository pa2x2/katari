package eu.kanade.tachiyomi.data.track.mangabaka

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant

class MangaBakaApiTest {

    @Test
    fun `response date accepts ISO instant`() {
        parseResponseDate("2026-07-11T12:34:56Z") shouldBe
            Instant.parse("2026-07-11T12:34:56Z").toEpochMilliseconds()
    }

    @Test
    fun `response date accepts ISO local date`() {
        parseResponseDate("2026-07-11") shouldBe LocalDate.parse("2026-07-11")
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }
}
