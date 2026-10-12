package mihon.entry.interactions.manga.download

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ChapterDirNamesTest {

    @Test
    fun `downloads stay found under every name an earlier setting or version wrote`() {
        // md5("/c/1") starts with 86c8db
        val names = ChapterDirNames(chapterName = "Ch. 1", scanlator = "Café.", url = "/c/1")
        val validNames = names.validDirNames(disallowNonAscii = false, appendHash = true)

        validNames.take(2) shouldBe listOf("Café._Ch. 1_86c8db", "Café._Ch. 1_86c8db.cbz")
        validNames shouldContainAll listOf(
            // Non-ASCII names disallowed
            "Cafc3a9._Ch. 1_86c8db",
            // Before the hash, or with it turned off
            "Café._Ch. 1",
            "Café._Ch. 1.cbz",
            // Mihon 0.21
            "Café_Ch. 1",
            "Café_Ch. 1_86c8db.cbz",
        )
        names.dirName(disallowNonAscii = false, appendHash = false) shouldBe "Café._Ch. 1"
        names.validDirNames(disallowNonAscii = true, appendHash = false) shouldContain "Café._Ch. 1_86c8db"
    }

    @Test
    fun `a long chapter name is cut together with its scanlator to stay within the name limit`() {
        val names = ChapterDirNames(chapterName = "a".repeat(300), scanlator = "Group", url = "/c/1")

        names.dirName(disallowNonAscii = false, appendHash = true) shouldBe "Group_" + "a".repeat(223) + "_86c8db"
        // Mihon 0.21 cuts only the chapter name
        names.validDirNames(disallowNonAscii = false, appendHash = true) shouldContain
            "Group_" + "a".repeat(229) + "_86c8db"
    }
}
