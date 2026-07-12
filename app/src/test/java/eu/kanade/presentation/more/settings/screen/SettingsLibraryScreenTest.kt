package eu.kanade.presentation.more.settings.screen

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SettingsLibraryScreenTest {

    @Test
    fun `unified profiles expose all library settings sections`() {
        visibleLibrarySettingsSections() shouldBe listOf(
            LibrarySettingsSection.Categories,
            LibrarySettingsSection.Display,
            LibrarySettingsSection.Group,
            LibrarySettingsSection.LibraryUpdate,
            LibrarySettingsSection.Behavior,
        )
    }
}
