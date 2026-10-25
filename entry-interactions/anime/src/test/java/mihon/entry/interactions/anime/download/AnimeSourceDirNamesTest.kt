package mihon.entry.interactions.anime.download

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.catalogue.EntryCatalogueSourceInfo
import mihon.entry.interactions.download.SourceDirNames
import org.junit.jupiter.api.Test

class AnimeSourceDirNamesTest {

    @Test
    fun `same-named anime sources get folders of their own and a manga source of that name gets none`() {
        // As Keiyoushi's and Yūzōnō's MyReadingManga, whose anime extension has a source per language
        val sources = listOf(
            source(id = 6318378620301107421, language = "en", type = EntryType.MANGA),
            source(id = -12, language = "en", type = EntryType.ANIME),
            source(id = -13, language = "ja", type = EntryType.ANIME),
        )

        sources.animeSourceDirNames(disallowNonAscii = false) shouldBe mapOf(
            -12L to SourceDirNames(current = "MyReadingManga (EN) [anime]", legacy = "MyReadingManga [anime]"),
            -13L to SourceDirNames(current = "MyReadingManga (JA) [anime]", legacy = "MyReadingManga [anime]"),
        )
    }

    private fun source(id: Long, language: String, type: EntryType) = EntryCatalogueSourceInfo(
        id = id,
        name = "MyReadingManga",
        language = language,
        supportedEntryTypes = setOf(type),
        itemOrientation = EntryItemOrientation.VERTICAL,
        supportsLatest = true,
    )
}
