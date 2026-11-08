package tachiyomi.core.metadata.comicinfo

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset

class ComicInfoDateTest {

    @Test
    fun `a chapter uploaded late in the evening reads back from the local source as the same day`() {
        val zone = ZoneId.of("America/Los_Angeles")
        // 2024-03-10 23:30 PDT, already March 11 in UTC
        val date = comicInfoDateOf(1_710_138_600_000L, zone)!!

        Triple(date.year.value, date.month.value, date.day.value) shouldBe Triple(2024, 3, 10)
        // 2024-03-10 00:00 PST
        comicInfo(date.year, date.month, date.day).uploadDateMillis(zone) shouldBe 1_710_057_600_000L
    }

    @Test
    fun `short years and out-of-range months still date the chapter`() {
        val utc = ZoneOffset.UTC

        // 0999-12-01
        comicInfo(ComicInfo.Year(999), ComicInfo.Month(13), null).uploadDateMillis(utc) shouldBe -30_612_902_400_000L
        // 2023-01-01
        comicInfo(ComicInfo.Year(2023), null, null).uploadDateMillis(utc) shouldBe 1_672_531_200_000L
        comicInfo(null, ComicInfo.Month(5), ComicInfo.Day(5)).uploadDateMillis(utc) shouldBe null
        comicInfo(ComicInfo.Year(2023), ComicInfo.Month(2), ComicInfo.Day(30)).uploadDateMillis(utc) shouldBe null
    }

    private fun comicInfo(year: ComicInfo.Year?, month: ComicInfo.Month?, day: ComicInfo.Day?) = ComicInfo(
        title = null,
        series = null,
        number = null,
        summary = null,
        writer = null,
        penciller = null,
        inker = null,
        colorist = null,
        letterer = null,
        coverArtist = null,
        translator = null,
        genre = null,
        tags = null,
        web = null,
        year = year,
        month = month,
        day = day,
        publishingStatus = null,
        categories = null,
        source = null,
    )
}
