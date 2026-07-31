package mihon.entry.interactions.reader.preparation

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.entry.viewer.settings.ReaderSharedSettingsRegistry
import mihon.entry.viewer.settings.StandardReaderCapabilities
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ReaderChapterPreparationSettingsProviderTest {
    private val mangaSurface = "builtin.manga"
    private val bookSurface = "builtin.book.document"
    private val unsupportedSurface = "builtin.book.fixed-layout"

    @Test
    fun `one profile preference is projected only to readers that prepare adjacent chapters`() {
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

        registry.rootSettings().map { it.id } shouldContainExactly
            listOf(ReaderChapterPreparationSettingsProvider.PREPARE_NEXT_CHAPTER_SETTING_ID)
        registry.settingsForSurface(mangaSurface).single().preference shouldBe preferences.prepareNextChapter
        registry.settingsForSurface(bookSurface).single().preference shouldBe preferences.prepareNextChapter
        registry.settingsForSurface(unsupportedSurface) shouldBe emptyList()
    }
}
