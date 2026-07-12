package eu.kanade.presentation.more.settings.screen

import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsProfileVisibilityTest {

    @Test
    fun `all settings screens are visible for unified profiles`() {
        isSettingsScreenVisible(SettingsReaderScreen) shouldBe true
        isSettingsScreenVisible(SettingsPlayerScreen) shouldBe true
        isSettingsScreenVisible(SettingsDownloadScreen) shouldBe true
        isSettingsScreenVisible(SettingsTrackingScreen) shouldBe true
        isSettingsScreenVisible(SettingsAppearanceScreen) shouldBe true
        isSettingsScreenVisible(SettingsLibraryScreen) shouldBe true
        isSettingsScreenVisible(SettingsBrowseScreen) shouldBe true
        isSettingsScreenVisible(SettingsDataScreen) shouldBe true
        isSettingsScreenVisible(SettingsSecurityScreen) shouldBe true
        isSettingsScreenVisible(SettingsProfilesScreen) shouldBe true
        isSettingsScreenVisible(SettingsAdvancedScreen) shouldBe true
        isSettingsScreenVisible(AboutScreen) shouldBe true
    }

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
    }
}
