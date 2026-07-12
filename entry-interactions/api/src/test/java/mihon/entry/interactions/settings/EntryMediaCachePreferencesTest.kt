package mihon.entry.interactions.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class EntryMediaCachePreferencesTest {

    @Test
    fun `media cache preferences keep independent global keys`() {
        val preferences = EntryMediaCachePreferences(InMemoryPreferenceStore())

        preferences.autoClearEntryPageImageCache.key() shouldBe "auto_clear_chapter_cache"
        preferences.autoClearAnimePlaybackCache.key() shouldBe "auto_clear_anime_playback_cache"
    }

    @Test
    fun `media cache auto-clear preferences default off and can be enabled independently`() {
        val preferences = EntryMediaCachePreferences(InMemoryPreferenceStore())

        preferences.autoClearEntryPageImageCache.get() shouldBe false
        preferences.autoClearAnimePlaybackCache.get() shouldBe false

        preferences.autoClearAnimePlaybackCache.set(true)

        preferences.autoClearEntryPageImageCache.get() shouldBe false
        preferences.autoClearAnimePlaybackCache.get() shouldBe true
    }

    @Test
    fun `legacy shared auto-clear setting also enables anime playback clearing`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("auto_clear_chapter_cache", true, false),
            ),
        )

        val preferences = EntryMediaCachePreferences(store)

        preferences.autoClearEntryPageImageCache.get() shouldBe true
        preferences.autoClearAnimePlaybackCache.get() shouldBe true
    }

    @Test
    fun `existing anime auto-clear choice is not overwritten by legacy setting`() {
        val store = InMemoryPreferenceStore(
            sequenceOf(
                InMemoryPreferenceStore.InMemoryPreference("auto_clear_chapter_cache", true, false),
                InMemoryPreferenceStore.InMemoryPreference("auto_clear_anime_playback_cache", false, false),
            ),
        )

        EntryMediaCachePreferences(store).autoClearAnimePlaybackCache.get() shouldBe false
    }
}
