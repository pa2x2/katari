package tachiyomi.domain.entry.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.entry.service.ChapterRecognition.ChapterNumbering
import tachiyomi.domain.entry.service.ChapterRecognition.SourceChapter

@Execution(ExecutionMode.CONCURRENT)
class ChapterRecognitionTest {

    @Test
    fun `ch prefix with volume yields decimal and alpha suffix numbers`() {
        val title = "Mokushiroku Alice"

        assertNumbers(title, "Mokushiroku Alice Vol.1 Ch.4: Misrepresentation" to ChapterNumbering("1", "4"))
        assertNumbers(title, "Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation" to ChapterNumbering("1", "4"))
        assertNumbers(title, "Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation" to ChapterNumbering("1", "4.1"))
        assertNumbers(title, "Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation" to ChapterNumbering("1", "4a"))
        assertNumbers(title, "Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation" to ChapterNumbering("1", "4b"))
        assertNumbers(title, "Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation" to ChapterNumbering("1", "4.99"))
    }

    @Test
    fun `number following the manga title yields decimal and alpha suffix numbers`() {
        assertNumbers("Bleach", "Bleach 567 Down With Snowwhite" to ChapterNumbering(null, "567"))
        assertNumbers("Bleach", "Bleach 567.4 Down With Snowwhite" to ChapterNumbering(null, "567.4"))
        assertNumbers("Bleach", "Bleach 567.b Down With Snowwhite" to ChapterNumbering(null, "567b"))
        assertNumbers("Solanin", "Solanin 028 Vol. 2" to ChapterNumbering("2", "28"))
        assertNumbers("Solanin", "Solanin 028.extra Vol. 2" to ChapterNumbering("2", "28.99"))
        assertNumbers("Onepunch-Man", "Onepunch-Man Punch Ver002 028" to ChapterNumbering(null, "28"))
        assertNumbers("Onepunch-Man", "Onepunch-Man Punch Ver002 028.1" to ChapterNumbering(null, "28.1"))
        assertNumbers("Onepunch-Man", "Onepunch-Man Punch Ver002 028.a" to ChapterNumbering(null, "28a"))
        assertNumbers("Onepunch-Man", "Onepunch-Man Punch Ver002 028.extra" to ChapterNumbering(null, "28.99"))
    }

    @Test
    fun `version markers are not mistaken for the chapter number`() {
        assertNumbers("random", "Vol.1 Ch.5v.2: Alones" to ChapterNumbering("1", "5"))
        assertNumbers(
            "Onepunch-Man",
            "Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]" to ChapterNumbering(null, "86"),
        )
        assertNumbers("Ansatsu Kyoushitsu", "Ansatsu Kyoushitsu 011v002: Assembly Time" to ChapterNumbering(null, "11"))
        assertNumbers("One-punch Man", "Mag Version 195.5" to ChapterNumbering(null, "195.5"))
    }

    @Test
    fun `numbers inside the manga title or chapter title are not mistaken for the chapter number`() {
        assertNumbers("Ayame 14", "Ayame 14 1 - The summer of 14" to ChapterNumbering(null, "1"))
        assertNumbers("Ayame 14", "Vol.1 Ch.1: March 25 (First Day Cohabiting)" to ChapterNumbering("1", "1"))
        assertNumbers("Tokyo ESP", "Tokyo ESP 027: Part 002: Chapter 001" to ChapterNumbering(null, "27b"))
        assertNumbers("random", "Fairy Tail 404: 00:00" to ChapterNumbering(null, "404"))
    }

    @Test
    fun `leading zeros alpha without dot and unparseable names`() {
        assertNumbers("random", "Vol.001 Ch.003: Kaguya Doesn't Know Much" to ChapterNumbering("1", "3"))
        assertNumbers("random", "Asu No Yoichi 19a" to ChapterNumbering(null, "19a"))
        assertNumbers("random", "Foo" to ChapterNumbering(null, null))
    }

    @Test
    fun `extra omake and special suffixes glued to a volume marker`() {
        val title = "Fairy Tail"

        assertNumbers(title, "Fairy Tail 404.extravol002" to ChapterNumbering(null, "404.99"))
        assertNumbers(title, "Fairy Tail 404 extravol002" to ChapterNumbering(null, "404.99"))
        assertNumbers(title, "Fairy Tail 404.omakevol002" to ChapterNumbering(null, "404.98"))
        assertNumbers(title, "Fairy Tail 404 omakevol002" to ChapterNumbering(null, "404.98"))
        assertNumbers(title, "Fairy Tail 404.specialvol002" to ChapterNumbering(null, "404.97"))
        assertNumbers(title, "Fairy Tail 404 specialvol002" to ChapterNumbering(null, "404.97"))
    }

    @Test
    fun `commas and hyphens act as decimal separators`() {
        assertNumbers("One Piece", "One Piece 300,a" to ChapterNumbering(null, "300a"))
        assertNumbers("One Piece", "One Piece Ch,123,extra" to ChapterNumbering(null, "123.99"))
        assertNumbers("One Piece", "One Piece the sunny, goes swimming 024,005" to ChapterNumbering(null, "24.005"))
        assertNumbers("Solo Leveling", "ch 122-a" to ChapterNumbering(null, "122a"))
        assertNumbers("Solo Leveling", "Solo Leveling Ch.123-extra" to ChapterNumbering(null, "123.99"))
        assertNumbers("Solo Leveling", "Solo Leveling, 024-005" to ChapterNumbering(null, "24.005"))
        assertNumbers("Solo Leveling", "Ch.191-200 Read Online" to ChapterNumbering(null, "191.2"))
    }

    @Test
    fun `volumes and seasons are recognized and not taken as the chapter number`() {
        assertNumbers("D.I.C.E", "D.I.C.E[Season 001] Ep. 007" to ChapterNumbering("1", "7"))
        assertNumbers("The Gamer", "S3 - Chapter 20" to ChapterNumbering("3", "20"))
        assertNumbers("Vol-3 Chapter 2" to ChapterNumbering("3", "2"))
        assertNumbers("Mokushiroku Alice", "Mokushiroku Alice Vol.3" to ChapterNumbering("3", null))
        assertNumbers("Mokushiroku Alice", "Vol.3 12" to ChapterNumbering("3", "12"))
        assertNumbers("One Outs", "One Outs 001" to ChapterNumbering(null, "1"))
        assertNumbers("Mob's 100" to ChapterNumbering(null, "100"))

        val title = "The Sister of the Woods with a Thousand Young"
        assertNumbers(title, "The 1st Night" to ChapterNumbering(null, "1"))
        assertNumbers(title, "The 4th Night" to ChapterNumbering(null, "4"))
    }

    @Test
    fun `parts become sub-chapter letters that count as tenths`() {
        assertNumbers("Chapter 1.5 Part I" to ChapterNumbering(null, "1.5a"))
        assertNumbers("Chapter 1.5 Part II" to ChapterNumbering(null, "1.5b"))
        assertNumbers("Chapter 12 Part 3" to ChapterNumbering(null, "12c"))
        assertNumbers("Vol.2 Chapter 7 - Part IV" to ChapterNumbering("2", "7d"))
        assertNumbers("Part 3" to ChapterNumbering(null, "3"))

        ChapterRecognition.toDouble("12") shouldBe 12.0
        ChapterRecognition.toDouble("10.5") shouldBe 10.5
        ChapterRecognition.toDouble("19a") shouldBe 19.1
        ChapterRecognition.toDouble("19i") shouldBe 19.9
        ChapterRecognition.toDouble("1.5a") shouldBe 1.5
        ChapterRecognition.toDouble("-2") shouldBe -2.0
    }

    @Test
    fun `numbers every similar name shares are ignored unless they follow a chapter prefix`() {
        assertNumbers(
            "Group 2 Chapter 387" to ChapterNumbering(null, "387"),
            "Group 2 Chapter 386" to ChapterNumbering(null, "386"),
            "Group 2 Chapter 385" to ChapterNumbering(null, "385"),
        )
        assertNumbers(
            "Chapter 5 Part 3" to ChapterNumbering(null, "5c"),
            "Chapter 5 Part 2" to ChapterNumbering(null, "5b"),
            "Chapter 5 Part 1" to ChapterNumbering(null, "5a"),
        )
    }

    @Test
    fun `chapters without a number are numbered between the chapters around them`() {
        assertNumbers(
            "Chapter 5" to ChapterNumbering(null, "5"),
            "Rain" to ChapterNumbering(null, "4"),
            "Snow" to ChapterNumbering(null, "3"),
            "Chapter 2" to ChapterNumbering(null, "2"),
        )
        assertNumbers(
            "Chapter 13" to ChapterNumbering(null, "13"),
            "Intermission" to ChapterNumbering(null, "10.5"),
            "Chapter 10" to ChapterNumbering(null, "10"),
        )
        assertNumbers(
            "Chapter 11" to ChapterNumbering(null, "11"),
            "Rain" to ChapterNumbering(null, "10.6"),
            "Snow" to ChapterNumbering(null, "10.5"),
            "Chapter 10" to ChapterNumbering(null, "10"),
        )
        assertNumbers(
            "Chapter 4" to ChapterNumbering(null, "4"),
            "Side Story" to ChapterNumbering(null, "2.5"),
            "Chapter 2" to ChapterNumbering(null, "2"),
        )
        assertNumbers(
            "Chapter 3" to ChapterNumbering(null, "3"),
            "Extra" to ChapterNumbering(null, "2.95"),
            "Chapter 2.9" to ChapterNumbering(null, "2.9"),
        )
    }

    @Test
    fun `only bonus chapters after the newest and a prologue before the oldest are numbered`() {
        assertNumbers(
            "Afterword" to ChapterNumbering(null, "100.5"),
            "Chapter 100" to ChapterNumbering(null, "100"),
        )
        assertNumbers(
            "Hiatus Announcement" to ChapterNumbering(null, null),
            "Chapter 100" to ChapterNumbering(null, "100"),
        )
        assertNumbers(
            "Chapter 1" to ChapterNumbering(null, "1"),
            "Prologue" to ChapterNumbering(null, "0"),
        )
        assertNumbers(
            "Chapter 1" to ChapterNumbering(null, "1"),
            "Character Profiles" to ChapterNumbering(null, null),
        )
        assertNumbers(
            "Vol.1 Ch.1" to ChapterNumbering("1", "1"),
            "Prologue" to ChapterNumbering("0", "0"),
        )
    }

    @Test
    fun `chapters without a number are only numbered within a volume`() {
        assertNumbers(
            "Vol.2 Ch.5" to ChapterNumbering("2", "5"),
            "Ch.4" to ChapterNumbering(null, "4"),
            "Vol.2 Ch.3" to ChapterNumbering("2", "3"),
        )
        assertNumbers(
            "Vol.1 Ch.3" to ChapterNumbering("1", "3"),
            "Rain" to ChapterNumbering(null, "2"),
            "Vol.1 Ch.1" to ChapterNumbering("1", "1"),
        )
        assertNumbers(
            "Vol.2 Ch.3" to ChapterNumbering("2", "3"),
            "Rain" to ChapterNumbering(null, null),
            "Vol.1 Ch.1" to ChapterNumbering("1", "1"),
        )
        assertNumbers(
            "Vol.2 Ch.3" to ChapterNumbering("2", "3"),
            "Vol.1 Rain" to ChapterNumbering("1", null),
            "Vol.2 Ch.1" to ChapterNumbering("2", "1"),
        )
        assertNumbers(
            "Vol.2 Extra" to ChapterNumbering("2", "10.5"),
            "Vol.2 Ch.10" to ChapterNumbering("2", "10"),
        )
        assertNumbers(
            "Vol.3 Extra" to ChapterNumbering("3", null),
            "Vol.2 Ch.10" to ChapterNumbering("2", "10"),
        )
    }

    @Test
    fun `numbers from the source are kept`() {
        val chapters = listOf(
            SourceChapter(name = "Vol.1 Chapter 3", chapterNumber = 7.0),
            SourceChapter(name = "Chapter 2", chapterNumber = -1.0),
        )

        ChapterRecognition.parseChapterNumbers("", chapters) shouldBe
            listOf(ChapterNumbering("1", "7"), ChapterNumbering(null, "2"))
    }

    private fun assertNumbers(vararg expected: Pair<String, ChapterNumbering>) = assertNumbers("", *expected)

    /**
     * Asserts the numbers recognized for chapters with the given names, listed from the most to the least recent.
     */
    private fun assertNumbers(entryTitle: String, vararg expected: Pair<String, ChapterNumbering>) {
        val chapters = expected.map { (name, _) -> SourceChapter(name = name, chapterNumber = -1.0) }
        ChapterRecognition.parseChapterNumbers(entryTitle, chapters) shouldBe expected.map { it.second }
    }
}
