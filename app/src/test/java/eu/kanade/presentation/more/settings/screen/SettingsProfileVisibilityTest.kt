package eu.kanade.presentation.more.settings.screen

import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsProfileVisibilityTest {

    @Test
    fun `direct destination stays intact for unified profiles`() {
        resolveSettingsStartScreen(
            destination = SettingsScreen.Destination.Tracking,
            twoPane = false,
        ) shouldBe SettingsTrackingScreen

        resolveSettingsStartScreen(
            destination = SettingsScreen.Destination.Tracking,
            twoPane = true,
        ) shouldBe SettingsTrackingScreen

        resolveSettingsStartScreen(
            destination = SettingsScreen.Destination.Translation,
            twoPane = false,
        ) shouldBe SettingsTranslationScreen

        resolveSettingsStartScreen(
            destination = SettingsScreen.Destination.Translation,
            twoPane = true,
        ) shouldBe SettingsTranslationScreen
    }
}
