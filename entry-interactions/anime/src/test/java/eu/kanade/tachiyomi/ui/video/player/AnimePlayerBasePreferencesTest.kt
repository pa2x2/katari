package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimePlayerBasePreferencesTest {

    @Test
    fun `immersive feed starts muted and remembers user choice`() {
        val store = InMemoryPreferenceStore()
        val preferences = AnimePlayerBasePreferences(store)

        preferences.immersiveFeedMuted.get() shouldBe true

        preferences.immersiveFeedMuted.set(false)

        preferences.immersiveFeedMuted.get() shouldBe false
    }
}
