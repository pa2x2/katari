package eu.kanade.tachiyomi.data.track.myanimelist

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class MyAnimeListApiTest {

    @Test
    fun `partial dates use the first day of their month or year`() {
        parseMyAnimeListDate("2024-11") shouldBe LocalDate.of(2024, 11, 1).toEpochMilliseconds()
        parseMyAnimeListDate("2025") shouldBe LocalDate.of(2025, 1, 1).toEpochMilliseconds()
    }
}

private fun LocalDate.toEpochMilliseconds(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
