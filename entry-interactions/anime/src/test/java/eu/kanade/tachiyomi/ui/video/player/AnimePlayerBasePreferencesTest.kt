package eu.kanade.tachiyomi.ui.video.player

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class AnimePlayerBasePreferencesTest {

    @Test
    fun `immersive feed mute state resets for each app session`() {
        val store = InMemoryPreferenceStore()
        val firstSession = AnimePlayerBasePreferences(store)

        firstSession.immersiveFeedMuted shouldBe true

        firstSession.immersiveFeedMuted = false

        firstSession.immersiveFeedMuted shouldBe false
        AnimePlayerBasePreferences(store).immersiveFeedMuted shouldBe true
    }
}
