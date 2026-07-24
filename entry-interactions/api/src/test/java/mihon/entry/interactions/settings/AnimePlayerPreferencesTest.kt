package mihon.entry.interactions.settings

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimePlayerPreferencesTest {

    @Test
    fun `anime player preferences keep legacy keys`() {
        val preferences = AnimePlayerPreferences(InMemoryPreferenceStore())

        preferences.enableAnimePictureInPicture.key() shouldBe "enable_anime_picture_in_picture"
        preferences.enableAnimeSeekPreview.key() shouldBe "enable_anime_seek_preview"
    }

    @Test
    fun `anime player preferences default off`() {
        val preferences = AnimePlayerPreferences(InMemoryPreferenceStore())

        preferences.enableAnimePictureInPicture.get() shouldBe false
        preferences.enableAnimeSeekPreview.get() shouldBe false
    }
}
