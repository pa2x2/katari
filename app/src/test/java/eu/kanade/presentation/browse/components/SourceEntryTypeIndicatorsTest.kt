package eu.kanade.presentation.browse.components

import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.extension.model.InstallStep
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.browse.ContentTypeFilter
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionFilterState
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionListState
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionUiModel
import eu.kanade.tachiyomi.ui.browse.extension.matches
import eu.kanade.tachiyomi.ui.browse.extension.supportedEntryTypesForDisplay
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

    @Test
    fun `content filter matches any selected advertised type`() {
        val extension = availableExtension(
            pkgName = "example.extension",
            sourceEntryTypes = setOf(EntryType.ANIME, EntryType.BOOK),
        )

        ContentTypeFilter(
            entryTypes = setOf(EntryType.MANGA, EntryType.BOOK),
        ).matches(extension) shouldBe true
        ContentTypeFilter(
            entryTypes = setOf(EntryType.MANGA),
        ).matches(extension) shouldBe false
    }

    @Test
    fun `unspecified filter explicitly controls extensions without metadata`() {
        val extension = availableExtension(
            pkgName = "example.extension",
            sourceEntryTypes = null,
        )

        ContentTypeFilter().matches(extension) shouldBe true
        ContentTypeFilter(
            entryTypes = setOf(EntryType.MANGA),
        ).matches(extension) shouldBe false
        ContentTypeFilter(
            includeUnspecified = true,
        ).matches(extension) shouldBe true
    }

    @Test
    fun `content filter preference value round trips known types and unspecified`() {
        val filter = ContentTypeFilter(
            entryTypes = setOf(EntryType.MANGA, EntryType.BOOK),
            includeUnspecified = true,
        )

        ContentTypeFilter.fromPreferenceValue(
            filter.toPreferenceValue() + "FUTURE_TYPE",
        ) shouldBe filter
    }

    @Test
    fun `active count represents content and language filter groups`() {
        ExtensionFilterState(
            languages = listOf("en", "ja"),
            enabledLanguages = setOf("en"),
            contentTypes = ContentTypeFilter(entryTypes = setOf(EntryType.BOOK)),
        ).activeFilterCount shouldBe 2
    }

    @Test
    fun `visible language count comes from filtered result items`() {
        val extension = availableExtension(
            pkgName = "example.extension",
            sourceEntryTypes = setOf(EntryType.BOOK),
        )
        val item = ExtensionUiModel.Item(extension, InstallStep.Idle)

        ExtensionListState(
            items = mapOf(ExtensionUiModel.Header.Text("English") to listOf(item)),
        ).visibleLanguageCount shouldBe 1
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
