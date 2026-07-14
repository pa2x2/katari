package eu.kanade.tachiyomi.ui.browse.catalog

import io.kotest.matchers.shouldBe
import mihon.core.common.CustomPreferences
import org.junit.jupiter.api.Test

class BrowseLongPressActionTest {

    @Test
    fun `immersive priority starts immersive view when source supports it`() {
        resolveBrowseLongPressAction(
            priority = priority(
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            ),
            supportsImmersive = true,
            previewEnabled = true,
        ) shouldBe CustomPreferences.BrowseLongPressAction.IMMERSIVE
    }

    @Test
    fun `unsupported immersive action falls through to preview`() {
        resolveBrowseLongPressAction(
            priority = priority(
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            ),
            supportsImmersive = false,
            previewEnabled = true,
        ) shouldBe CustomPreferences.BrowseLongPressAction.PREVIEW
    }

    @Test
    fun `unavailable immersive and preview actions fall through to library action`() {
        resolveBrowseLongPressAction(
            priority = priority(
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            ),
            supportsImmersive = false,
            previewEnabled = false,
        ) shouldBe CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION
    }

    private fun priority(
        vararg actions: CustomPreferences.BrowseLongPressAction,
    ): List<CustomPreferences.BrowseLongPressAction> {
        return actions.toList()
    }
}
