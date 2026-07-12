package mihon.entry.interactions.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class EntryInteractionPreferencesTest {

    @Test
    fun `manga preview page count clamps stored values to shared range`() {
        val key = EntryInteractionPreferences.MANGA_PREVIEW_PAGE_COUNT_KEY
        val preferenceStore = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference(key, 75, 5),
            ),
        )

        val preferences = EntryInteractionPreferences(preferenceStore)

        preferences.mangaPreviewPageCount.get() shouldBe 50
    }

    @Test
    fun `manga preview page count clamps writes to shared range`() {
        val preferenceStore = InMemoryPreferenceStore()
        val preferences = EntryInteractionPreferences(preferenceStore)

        preferences.mangaPreviewPageCount.set(75)

        preferences.mangaPreviewPageCount.get() shouldBe 50
    }
}
