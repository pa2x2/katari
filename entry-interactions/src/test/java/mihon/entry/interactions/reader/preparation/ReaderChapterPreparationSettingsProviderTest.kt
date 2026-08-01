package mihon.entry.interactions.reader.preparation

import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.entry.viewer.settings.shared.StandardReaderCapabilities
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderChapterPreparationSettingsProviderTest {
    private val mangaSurface = "builtin.manga.reader"
    private val bookSurface = "builtin.book.document"
    private val unsupportedSurface = "builtin.book.fixed-layout"

    @Test
    fun `independent profile preferences are projected only to readers that prepare adjacent chapters`() {
        val preferences = ReaderChapterPreparationPreferences(InMemoryPreferenceStore())
        val provider = ReaderChapterPreparationSettingsProvider(
            preferences = preferences,
            potentialCapabilitiesBySettingsSurface = mapOf(
                mangaSurface to setOf(StandardReaderCapabilities.NextChapterPreparation),
                bookSurface to setOf(StandardReaderCapabilities.NextChapterPreparation),
                unsupportedSurface to setOf(StandardReaderCapabilities.StableTextSelection),
            ),
        )
        val registry = ReaderSharedSettingsRegistry(listOf(provider))

        registry.rootSettings() shouldBe emptyList()
        registry.settingsForSurface(mangaSurface).single().preference shouldBe
            preferences.prepareNextChapter(mangaSurface)
        registry.settingsForSurface(bookSurface).single().preference shouldBe
            preferences.prepareNextChapter(bookSurface)
        registry.settingsForSurface(unsupportedSurface) shouldBe emptyList()
    }
}
