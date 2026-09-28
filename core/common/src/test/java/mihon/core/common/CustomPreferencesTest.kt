package mihon.core.common

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CustomPreferencesTest {

    @Test
    fun `legacy browse long press choice becomes first priority and missing actions are appended`() {
        "MANGA_PREVIEW".toBrowseLongPressActionPriority() shouldBe listOf(
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
        )
    }

    @Test
    fun `browse long press action priority removes duplicates and round trips`() {
        val priority = listOf(
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
        )

        priority.toBrowseLongPressActionPriorityPreferenceValue().toBrowseLongPressActionPriority() shouldBe listOf(
            CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            CustomPreferences.BrowseLongPressAction.PREVIEW,
            CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
        )
    }

    @Test
    fun `browse long press source overrides ignore malformed stored values`() {
        "invalid:IMMERSIVE;30:UNKNOWN;40:PREVIEW".toBrowseLongPressActionOverrides() shouldBe mapOf(
            40L to listOf(
                CustomPreferences.BrowseLongPressAction.PREVIEW,
                CustomPreferences.BrowseLongPressAction.LIBRARY_ACTION,
                CustomPreferences.BrowseLongPressAction.IMMERSIVE,
            ),
        )
    }
}
