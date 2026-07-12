package eu.kanade.tachiyomi.ui.main

import eu.kanade.tachiyomi.ui.home.HomeScreen
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.Constants

class MainActivityShortcutIntentTest {

    @Test
    fun `anime shortcut resolves to library entry tab`() {
        resolveShortcutTab(
            action = Constants.SHORTCUT_ANIME,
            entryIdToOpen = 42L,
        ) shouldBe HomeScreen.Tab.Library(entryIdToOpen = 42L)
    }

    @Test
    fun `manga shortcut resolves to library entry tab`() {
        resolveShortcutTab(
            action = Constants.SHORTCUT_MANGA,
            entryIdToOpen = 24L,
        ) shouldBe HomeScreen.Tab.Library(entryIdToOpen = 24L)
    }
}
