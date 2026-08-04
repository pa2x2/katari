package eu.kanade.tachiyomi.data.track.myanimelist

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class MyAnimeListApiTest {

    @Test
    fun `year and month date uses first day of month`() {
        parseMyAnimeListDate("2024-11") shouldBe LocalDate.of(2024, 11, 1).toEpochMilliseconds()
    }

    @Test
    fun `year date uses first day of year`() {
        parseMyAnimeListDate("2025") shouldBe LocalDate.of(2025, 1, 1).toEpochMilliseconds()
    }
}

private fun LocalDate.toEpochMilliseconds(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
