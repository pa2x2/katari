package mihon.entry.interactions.manga.reader.settings

import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.shared.StandardReaderSharedSettingIds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class MangaReaderSettingsProviderTest {

    @Test
    fun `library and app-only settings stay profile scoped`() {
        val provider = provider()

        val profileOnlyKeys = provider.settings
            .filter { it.scope == ViewerSettingScope.PROFILE_ONLY }
            .map { it.id.key }
            .toSet()

        assertEquals(LIBRARY_AND_APP_ONLY_SETTING_KEYS, profileOnlyKeys)
    }

    @Test
    fun `in-reader navigation and appearance settings support entry overrides`() {
        val provider = provider()

        val overridableKeys = provider.settings
            .filter { it.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE }
            .map { it.id.key }
            .toSet()

        assertEquals(OVERRIDABLE_IN_READER_SETTING_KEYS, overridableKeys)
    }

    @Test
    fun `prepare next chapter is declared as an entry-overridable shared setting`() = runTest {
        val provider = provider()

        val definition = provider.sharedSettingDefinitions
            .getValue(StandardReaderSharedSettingIds.NextChapterPreparation)

        assertTrue(definition in provider.settings)
        assertEquals(ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE, definition.scope)
        assertFalse(definition.processorDefault)
    }

    @Test
    fun `definition ids belong to the manga reader surface`() {
        val provider = provider()

        assertEquals(MangaReaderSettings.PROVIDER_ID, provider.id)
        assertTrue(provider.settings.all { it.id.providerId == MangaReaderSettings.PROVIDER_ID })
        assertEquals(
            provider.settings.size,
            provider.settings.map { it.id }.distinct().size,
        )
    }

    private fun provider() = MangaReaderSettingsProvider(
        preferenceStore = InMemoryPreferenceStore(),
        chapterPreparationPreferences = ReaderChapterPreparationPreferences(InMemoryPreferenceStore()),
    )

    private companion object {
        val LIBRARY_AND_APP_ONLY_SETTING_KEYS = setOf(
            "skip_read",
            "skip_filtered",
            "skip_duplicate",
            "folder_per_manga",
            "show_reading_mode",
            "show_navigation_overlay_on_start",
            "show_navigation_overlay_new_user",
            "double_tap_animation_millis",
            "read_with_volume_keys",
            "read_with_volume_keys_inverted",
            "auto_scroll_enabled",
            "auto_scroll_speed",
        )

        val OVERRIDABLE_IN_READER_SETTING_KEYS = setOf(
            "reading_mode",
            "orientation",
            "chapter_transition",
            "reader.prepare-next-chapter",
            "page_transitions",
            "read_with_long_tap",
            "reader_theme",
            "show_page_number",
            "fullscreen",
            "draw_under_cutout",
            "keep_screen_on",
            "flash_on_page_change",
            "flash_duration_millis",
            "flash_page_interval",
            "flash_color",
            "navigation_mode_pager",
            "pager_navigation_inverted",
            "image_scale_type",
            "zoom_start",
            "crop_borders",
            "landscape_zoom",
            "navigate_to_pan",
            "dual_page_split_paged",
            "dual_page_invert_paged",
            "dual_page_rotate_to_fit",
            "dual_page_rotate_to_fit_invert",
            "navigation_mode_webtoon",
            "webtoon_navigation_inverted",
            "webtoon_side_padding",
            "reader_hide_threshold",
            "crop_borders_webtoon",
            "dual_page_split_webtoon",
            "dual_page_invert_webtoon",
            "dual_page_rotate_to_fit_webtoon",
            "dual_page_rotate_to_fit_invert_webtoon",
            "webtoon_double_tap_zoom",
            "webtoon_disable_zoom_out",
            "vertical_navigator_modes",
            "vertical_navigator_on_left",
            "vertical_navigator_height",
            "custom_brightness",
            "custom_brightness_value",
            "color_filter",
            "color_filter_value",
            "color_filter_mode",
            "grayscale",
            "inverted_colors",
        )
    }
}
