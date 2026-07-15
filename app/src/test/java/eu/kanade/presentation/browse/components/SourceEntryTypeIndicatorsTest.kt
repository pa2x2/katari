package eu.kanade.presentation.browse.components

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.domain.extension.model.ExtensionStore
import org.junit.jupiter.api.Test

class SourceEntryTypeIndicatorsTest {

    @Test
    fun `available extension aggregates repository source metadata`() {
        val supportedTypes = setOf(EntryType.ANIME, EntryType.MANGA, EntryType.BOOK)
        availableExtension(
            pkgName = "example.extension",
            sourceEntryTypes = supportedTypes,
        ).supportedEntryTypesForDisplay() shouldBe supportedTypes
    }

    private fun availableExtension(
        pkgName: String,
        sourceEntryTypes: Set<EntryType>?,
    ) = Extension.Available(
        name = "Example",
        pkgName = pkgName,
        versionName = "2.0.1",
        versionCode = 1L,
        libVersion = 2.0,
        lang = "en",
        isNsfw = false,
        sources = listOf(
            Extension.Available.Source(
                id = 1L,
                lang = "en",
                name = "Example",
                baseUrl = "https://example.invalid",
                supportedEntryTypes = sourceEntryTypes,
            ),
        ),
        apkUrl = "",
        iconUrl = "",
        store = store,
        libVersionName = "2.0.0",
    )

    private companion object {
        val store = ExtensionStore(
            indexUrl = "https://example.invalid/index.json",
            name = "Example",
            badgeLabel = "Example",
            signingKey = "",
            contact = ExtensionStore.Contact(
                website = "https://example.invalid",
                discord = null,
            ),
            isLegacy = false,
        )
    }
}
