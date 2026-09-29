package tachiyomi.domain.chapter.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.entry.service.ChapterRecognition

@Execution(ExecutionMode.CONCURRENT)
class ChapterRecognitionTest {

    @Test
    fun `ch prefix with volume yields decimal and alpha suffix numbers`() {
        val title = "Mokushiroku Alice"

        assertChapter(title, "Mokushiroku Alice Vol.1 Ch.4: Misrepresentation", 4.0)
        assertChapter(title, "Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation", 4.0)
        assertChapter(title, "Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation", 4.1)
        assertChapter(title, "Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation", 4.1)
        assertChapter(title, "Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation", 4.2)
        assertChapter(title, "Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation", 4.99)
    }

    @Test
    fun `number following the manga title yields decimal and alpha suffix numbers`() {
        assertChapter("Bleach", "Bleach 567 Down With Snowwhite", 567.0)
        assertChapter("Bleach", "Bleach 567.4 Down With Snowwhite", 567.4)
        assertChapter("Bleach", "Bleach 567.b Down With Snowwhite", 567.2)
        assertChapter("Solanin", "Solanin 028 Vol. 2", 28.0)
        assertChapter("Solanin", "Solanin 028.extra Vol. 2", 28.99)
        assertChapter("Onepunch-Man", "Onepunch-Man Punch Ver002 028", 28.0)
        assertChapter("Onepunch-Man", "Onepunch-Man Punch Ver002 028.1", 28.1)
        assertChapter("Onepunch-Man", "Onepunch-Man Punch Ver002 028.a", 28.1)
        assertChapter("Onepunch-Man", "Onepunch-Man Punch Ver002 028.extra", 28.99)
    }

    @Test
    fun `version markers are not mistaken for the chapter number`() {
        assertChapter("random", "Vol.1 Ch.5v.2: Alones", 5.0)
        assertChapter("Onepunch-Man", "Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]", 86.0)
        assertChapter("Ansatsu Kyoushitsu", "Ansatsu Kyoushitsu 011v002: Assembly Time", 11.0)
        assertChapter("One-punch Man", "Mag Version 195.5", 195.5)
    }

    @Test
    fun `numbers inside the manga title or chapter title are not mistaken for the chapter number`() {
        assertChapter("Ayame 14", "Ayame 14 1 - The summer of 14", 1.0)
        assertChapter("Ayame 14", "Vol.1 Ch.1: March 25 (First Day Cohabiting)", 1.0)
        assertChapter("Tokyo ESP", "Tokyo ESP 027: Part 002: Chapter 001", 27.0)
        assertChapter("random", "Fairy Tail 404: 00:00", 404.0)
    }

    @Test
    fun `leading zeros alpha without dot and unparseable names`() {
        assertChapter("random", "Vol.001 Ch.003: Kaguya Doesn't Know Much", 3.0)
        assertChapter("random", "Asu No Yoichi 19a", 19.1)
        assertChapter("random", "Foo", -1.0)
    }

    @Test
    fun `extra omake and special suffixes glued to a volume marker`() {
        val title = "Fairy Tail"

        assertChapter(title, "Fairy Tail 404.extravol002", 404.99)
        assertChapter(title, "Fairy Tail 404 extravol002", 404.99)
        assertChapter(title, "Fairy Tail 404.omakevol002", 404.98)
        assertChapter(title, "Fairy Tail 404 omakevol002", 404.98)
        assertChapter(title, "Fairy Tail 404.specialvol002", 404.97)
        assertChapter(title, "Fairy Tail 404 specialvol002", 404.97)
    }

    @Test
    fun `commas and hyphens act as decimal separators`() {
        assertChapter("One Piece", "One Piece 300,a", 300.1)
        assertChapter("One Piece", "One Piece Ch,123,extra", 123.99)
        assertChapter("One Piece", "One Piece the sunny, goes swimming 024,005", 24.005)
        assertChapter("Solo Leveling", "ch 122-a", 122.1)
        assertChapter("Solo Leveling", "Solo Leveling Ch.123-extra", 123.99)
        assertChapter("Solo Leveling", "Solo Leveling, 024-005", 24.005)
        assertChapter("Solo Leveling", "Ch.191-200 Read Online", 191.200)
    }

    @Test
    fun `season prefixes trailing s and ordinals`() {
        assertChapter("D.I.C.E", "D.I.C.E[Season 001] Ep. 007", 7.0)
        assertChapter("The Gamer", "S3 - Chapter 20", 20.0)
        assertChapter("One Outs", "One Outs 001", 1.0)

        val title = "The Sister of the Woods with a Thousand Young"
        assertChapter(title, "The 1st Night", 1.0)
        assertChapter(title, "The 2nd Night", 2.0)
        assertChapter(title, "The 3rd Night", 3.0)
        assertChapter(title, "The 4th Night", 4.0)
    }

    private fun assertChapter(mangaTitle: String, name: String, expected: Double) {
        ChapterRecognition.parseChapterNumber(mangaTitle, name) shouldBe expected
    }
}
