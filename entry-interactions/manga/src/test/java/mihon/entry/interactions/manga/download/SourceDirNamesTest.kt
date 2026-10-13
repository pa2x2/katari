package mihon.entry.interactions.manga.download

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceDirNamesTest {

    @Test
    fun `only a legacy folder of one source without a current folder is renamed`() {
        val folders = listOf(
            // Katari 1.15, one source
            "Asura Scans",
            // Shared by the languages of a multi-language extension
            "MangaDex",
            // Katari 1.15 next to the Mihon folder of the same source
            "comick",
            "Comick (EN)",
        )
        val sources = listOf(
            SourceDirNames(current = "Asura Scans (EN)", legacy = "Asura Scans"),
            SourceDirNames(current = "MangaDex (EN)", legacy = "MangaDex"),
            SourceDirNames(current = "MangaDex (ES)", legacy = "MangaDex"),
            SourceDirNames(current = "Comick (EN)", legacy = "Comick"),
        )

        legacySourceDirRenames(folders, sources) shouldBe mapOf("Asura Scans" to "Asura Scans (EN)")
    }
}
