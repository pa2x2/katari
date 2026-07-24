package mihon.entry.interactions.reader.settings

import android.os.Build
import androidx.compose.ui.graphics.BlendMode
import dev.icerock.moko.resources.StringResource
import mihon.entry.viewer.settings.ViewerSettingCodecs
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsCategory
import mihon.entry.viewer.settings.ViewerSettingsProvider
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.coerceIn
import tachiyomi.core.common.preference.getEnum
import tachiyomi.core.common.preference.getEnumSet
import tachiyomi.i18n.MR

class MangaReaderSettingsProvider(
    preferenceStore: PreferenceStore,
) : ViewerSettingsProvider {

    override val id: String = PROVIDER_ID
    override val category: ViewerSettingsCategory = ViewerSettingsCategory.READER
    override val displayName: String = "Manga reader"

    // region General

    val pageTransitions: Preference<Boolean> = preferenceStore.getBoolean("pref_enable_transitions_key", true)

    val flashOnPageChange: Preference<Boolean> = preferenceStore.getBoolean("pref_reader_flash", false)

    val flashDurationMillis: Preference<Int> = preferenceStore.getInt("pref_reader_flash_duration", MILLI_CONVERSION)

    val flashPageInterval: Preference<Int> = preferenceStore.getInt("pref_reader_flash_interval", 1)

    val flashColor: Preference<FlashColor> = preferenceStore.getEnum("pref_reader_flash_mode", FlashColor.BLACK)

    val doubleTapAnimSpeed: Preference<Int> = preferenceStore.getInt("pref_double_tap_anim_speed", 500)

    val showPageNumber: Preference<Boolean> = preferenceStore.getBoolean("pref_show_page_number_key", true)

    val verticalNavigator: Preference<Set<ReadingMode>> = preferenceStore.getEnumSet(
        "pref_vertical_navigator",
        emptySet(),
    )

    val verticalNavigatorOnLeft: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_vertical_navigator_on_left",
        false,
    )

    val verticalNavigatorHeight: Preference<Int> = preferenceStore.getInt(
        "pref_vertical_navigator_height",
        65,
    )

    val showReadingMode: Preference<Boolean> = preferenceStore.getBoolean("pref_show_reading_mode", true)

    val fullscreen: Preference<Boolean> = preferenceStore.getBoolean("fullscreen", true)

    val drawUnderCutout: Preference<Boolean> = preferenceStore.getBoolean("cutout_short", true)

    val keepScreenOn: Preference<Boolean> = preferenceStore.getBoolean("pref_keep_screen_on_key", false)

    val defaultReadingMode: Preference<Int> = preferenceStore.getInt(
        "pref_default_reading_mode_key",
        ReadingMode.RIGHT_TO_LEFT.flagValue,
    )

    val defaultOrientationType: Preference<Int> = preferenceStore.getInt(
        "pref_default_orientation_type_key",
        ReaderOrientation.FREE.flagValue,
    )

    val readingModeSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, READING_MODE_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = ReadingMode.RIGHT_TO_LEFT.flagValue,
        profilePreference = defaultReadingMode,
        codec = ViewerSettingCodecs.Int,
        validate = { value -> ReadingMode.entries.any { it.flagValue == value } },
    )

    val orientationSetting = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, ORIENTATION_KEY),
        scope = ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE,
        processorDefault = ReaderOrientation.FREE.flagValue,
        profilePreference = defaultOrientationType,
        codec = ViewerSettingCodecs.Int,
        validate = { value -> ReaderOrientation.entries.any { it.flagValue == value } },
    )

    val webtoonDoubleTapZoomEnabled: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_enable_double_tap_zoom_webtoon",
        true,
    )

    val imageScaleType: Preference<Int> = preferenceStore.getInt("pref_image_scale_type_key", 1)

    val zoomStart: Preference<Int> = preferenceStore.getInt("pref_zoom_start_key", 1)

    val readerTheme: Preference<Int> = preferenceStore.getInt("pref_reader_theme_key", 1)

    val alwaysShowChapterTransition: Preference<Boolean> = preferenceStore.getBoolean(
        "always_show_chapter_transition",
        true,
    )

    val cropBorders: Preference<Boolean> = preferenceStore.getBoolean("crop_borders", false)

    val navigateToPan: Preference<Boolean> = preferenceStore.getBoolean("navigate_pan", true)

    val landscapeZoom: Preference<Boolean> = preferenceStore.getBoolean("landscape_zoom", true)

    val cropBordersWebtoon: Preference<Boolean> = preferenceStore.getBoolean("crop_borders_webtoon", false)

    val webtoonSidePadding: Preference<Int> = preferenceStore.getInt("webtoon_side_padding", WEBTOON_PADDING_MIN)

    val readerHideThreshold: Preference<ReaderHideThreshold> = preferenceStore.getEnum(
        "reader_hide_threshold",
        ReaderHideThreshold.LOW,
    )

    val folderPerManga: Preference<Boolean> = preferenceStore.getBoolean("create_folder_per_manga", false)

    val skipRead: Preference<Boolean> = preferenceStore.getBoolean("skip_read", false)

    val skipFiltered: Preference<Boolean> = preferenceStore.getBoolean("skip_filtered", true)

    val skipDupe: Preference<Boolean> = preferenceStore.getBoolean("skip_dupe", false)

    val webtoonDisableZoomOut: Preference<Boolean> = preferenceStore.getBoolean("webtoon_disable_zoom_out", false)

    val autoScrollEnabled: Preference<Boolean> = preferenceStore.getBoolean("reader_auto_scroll", false)

    val autoScrollSpeed: Preference<Int> = preferenceStore.getInt(
        "reader_auto_scroll_speed",
        AUTO_SCROLL_LEVEL_DEFAULT,
    ).coerceIn(AUTO_SCROLL_SPEED_RANGE)

    // endregion

    // region Split two-page spread

    val dualPageSplitPaged: Preference<Boolean> = preferenceStore.getBoolean("pref_dual_page_split", false)

    val dualPageInvertPaged: Preference<Boolean> = preferenceStore.getBoolean("pref_dual_page_invert", false)

    val dualPageSplitWebtoon: Preference<Boolean> = preferenceStore.getBoolean("pref_dual_page_split_webtoon", false)

    val dualPageInvertWebtoon: Preference<Boolean> = preferenceStore.getBoolean("pref_dual_page_invert_webtoon", false)

    val dualPageRotateToFit: Preference<Boolean> = preferenceStore.getBoolean("pref_dual_page_rotate", false)

    val dualPageRotateToFitInvert: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_dual_page_rotate_invert",
        false,
    )

    val dualPageRotateToFitWebtoon: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_dual_page_rotate_webtoon",
        false,
    )

    val dualPageRotateToFitInvertWebtoon: Preference<Boolean> = preferenceStore.getBoolean(
        "pref_dual_page_rotate_invert_webtoon",
        false,
    )

    // endregion

    // region Color filter

    val customBrightness: Preference<Boolean> = preferenceStore.getBoolean("pref_custom_brightness_key", false)

    val customBrightnessValue: Preference<Int> = preferenceStore.getInt("custom_brightness_value", 0)

    val colorFilter: Preference<Boolean> = preferenceStore.getBoolean("pref_color_filter_key", false)

    val colorFilterValue: Preference<Int> = preferenceStore.getInt("color_filter_value", 0)

    val colorFilterMode: Preference<Int> = preferenceStore.getInt("color_filter_mode", 0)

    val grayscale: Preference<Boolean> = preferenceStore.getBoolean("pref_grayscale", false)

    val invertedColors: Preference<Boolean> = preferenceStore.getBoolean("pref_inverted_colors", false)

    // endregion

    // region Controls

    val readWithLongTap: Preference<Boolean> = preferenceStore.getBoolean("reader_long_tap", true)

    val readWithVolumeKeys: Preference<Boolean> = preferenceStore.getBoolean("reader_volume_keys", false)

    val readWithVolumeKeysInverted: Preference<Boolean> = preferenceStore.getBoolean(
        "reader_volume_keys_inverted",
        false,
    )

    val navigationModePager: Preference<Int> = preferenceStore.getInt("reader_navigation_mode_pager", 0)

    val navigationModeWebtoon: Preference<Int> = preferenceStore.getInt("reader_navigation_mode_webtoon", 0)

    val pagerNavInverted: Preference<TappingInvertMode> = preferenceStore.getEnum(
        "reader_tapping_inverted",
        TappingInvertMode.NONE,
    )

    val webtoonNavInverted: Preference<TappingInvertMode> = preferenceStore.getEnum(
        "reader_tapping_inverted_webtoon",
        TappingInvertMode.NONE,
    )

    val showNavigationOverlayNewUser: Preference<Boolean> = preferenceStore.getBoolean(
        "reader_navigation_overlay_new_user",
        true,
    )

    val showNavigationOverlayOnStart: Preference<Boolean> = preferenceStore.getBoolean(
        "reader_navigation_overlay_on_start",
        false,
    )

    override val settings: List<ViewerSettingDefinition<*>> = listOf(
        readingModeSetting,
        orientationSetting,
        booleanSetting("page_transitions", pageTransitions),
        booleanSetting("flash_on_page_change", flashOnPageChange),
        intSetting("flash_duration_millis", flashDurationMillis) { it >= 0 },
        intSetting("flash_page_interval", flashPageInterval) { it > 0 },
        profileOnly("flash_color", flashColor, enumCodec(FlashColor.entries)),
        intSetting("double_tap_animation_millis", doubleTapAnimSpeed) { it >= 0 },
        booleanSetting("show_page_number", showPageNumber),
        profileOnly("vertical_navigator_modes", verticalNavigator, readingModeSetCodec()),
        booleanSetting("vertical_navigator_on_left", verticalNavigatorOnLeft),
        intSetting("vertical_navigator_height", verticalNavigatorHeight) { it in 0..100 },
        booleanSetting("show_reading_mode", showReadingMode),
        booleanSetting("fullscreen", fullscreen),
        booleanSetting("draw_under_cutout", drawUnderCutout),
        booleanSetting("keep_screen_on", keepScreenOn),
        booleanSetting("webtoon_double_tap_zoom", webtoonDoubleTapZoomEnabled),
        intSetting("image_scale_type", imageScaleType) { it in ImageScaleType.indices },
        intSetting("zoom_start", zoomStart) { it in ZoomStart.indices },
        intSetting("reader_theme", readerTheme),
        booleanSetting("always_show_chapter_transition", alwaysShowChapterTransition),
        booleanSetting("crop_borders", cropBorders),
        booleanSetting("navigate_to_pan", navigateToPan),
        booleanSetting("landscape_zoom", landscapeZoom),
        booleanSetting("crop_borders_webtoon", cropBordersWebtoon),
        intSetting("webtoon_side_padding", webtoonSidePadding) { it in WEBTOON_PADDING_MIN..WEBTOON_PADDING_MAX },
        profileOnly("reader_hide_threshold", readerHideThreshold, enumCodec(ReaderHideThreshold.entries)),
        booleanSetting("folder_per_manga", folderPerManga),
        booleanSetting("skip_read", skipRead),
        booleanSetting("skip_filtered", skipFiltered),
        booleanSetting("skip_duplicate", skipDupe),
        booleanSetting("webtoon_disable_zoom_out", webtoonDisableZoomOut),
        booleanSetting("auto_scroll_enabled", autoScrollEnabled),
        intSetting("auto_scroll_speed", autoScrollSpeed) { it in AUTO_SCROLL_SPEED_RANGE },
        booleanSetting("dual_page_split_paged", dualPageSplitPaged),
        booleanSetting("dual_page_invert_paged", dualPageInvertPaged),
        booleanSetting("dual_page_split_webtoon", dualPageSplitWebtoon),
        booleanSetting("dual_page_invert_webtoon", dualPageInvertWebtoon),
        booleanSetting("dual_page_rotate_to_fit", dualPageRotateToFit),
        booleanSetting("dual_page_rotate_to_fit_invert", dualPageRotateToFitInvert),
        booleanSetting("dual_page_rotate_to_fit_webtoon", dualPageRotateToFitWebtoon),
        booleanSetting("dual_page_rotate_to_fit_invert_webtoon", dualPageRotateToFitInvertWebtoon),
        booleanSetting("custom_brightness", customBrightness),
        intSetting("custom_brightness_value", customBrightnessValue),
        booleanSetting("color_filter", colorFilter),
        intSetting("color_filter_value", colorFilterValue),
        intSetting("color_filter_mode", colorFilterMode) { it in ColorFilterMode.indices },
        booleanSetting("grayscale", grayscale),
        booleanSetting("inverted_colors", invertedColors),
        booleanSetting("read_with_long_tap", readWithLongTap),
        booleanSetting("read_with_volume_keys", readWithVolumeKeys),
        booleanSetting("read_with_volume_keys_inverted", readWithVolumeKeysInverted),
        intSetting("navigation_mode_pager", navigationModePager) { it in TapZones.indices },
        intSetting("navigation_mode_webtoon", navigationModeWebtoon) { it in TapZones.indices },
        profileOnly("pager_navigation_inverted", pagerNavInverted, enumCodec(TappingInvertMode.entries)),
        profileOnly("webtoon_navigation_inverted", webtoonNavInverted, enumCodec(TappingInvertMode.entries)),
        booleanSetting("show_navigation_overlay_new_user", showNavigationOverlayNewUser),
        booleanSetting("show_navigation_overlay_on_start", showNavigationOverlayOnStart),
    )

    private fun booleanSetting(key: String, preference: Preference<Boolean>) = profileOnly(
        key = key,
        preference = preference,
        codec = ViewerSettingCodecs.Boolean,
    )

    private fun intSetting(
        key: String,
        preference: Preference<Int>,
        validate: (Int) -> Boolean = { true },
    ) = profileOnly(
        key = key,
        preference = preference,
        codec = ViewerSettingCodecs.Int,
        validate = validate,
    )

    private fun <T> profileOnly(
        key: String,
        preference: Preference<T>,
        codec: mihon.entry.viewer.settings.ViewerSettingCodec<T>,
        validate: (T) -> Boolean = { true },
    ) = ViewerSettingDefinition(
        id = ViewerSettingId(PROVIDER_ID, key),
        scope = ViewerSettingScope.PROFILE_ONLY,
        processorDefault = preference.defaultValue(),
        profilePreference = preference,
        codec = codec,
        validate = validate,
    )

    private fun <T : Enum<T>> enumCodec(values: List<T>) = ViewerSettingCodecs.codec<T>(
        encode = Enum<T>::name,
        decode = { encoded -> values.firstOrNull { it.name == encoded } },
    )

    private fun readingModeSetCodec() = ViewerSettingCodecs.codec<Set<ReadingMode>>(
        encode = { values -> values.map(ReadingMode::name).sorted().joinToString(",") },
        decode = { encoded ->
            if (encoded.isEmpty()) {
                emptySet()
            } else {
                encoded.split(',').map { name ->
                    ReadingMode.entries.firstOrNull { it.name == name } ?: return@codec null
                }.toSet()
            }
        },
    )

    // endregion

    enum class FlashColor {
        BLACK,
        WHITE,
        WHITE_BLACK,
    }

    enum class TappingInvertMode(
        val titleRes: StringResource,
        val shouldInvertHorizontal: Boolean = false,
        val shouldInvertVertical: Boolean = false,
    ) {
        NONE(MR.strings.tapping_inverted_none),
        HORIZONTAL(MR.strings.tapping_inverted_horizontal, shouldInvertHorizontal = true),
        VERTICAL(MR.strings.tapping_inverted_vertical, shouldInvertVertical = true),
        BOTH(MR.strings.tapping_inverted_both, shouldInvertHorizontal = true, shouldInvertVertical = true),
    }

    enum class ReaderHideThreshold(val threshold: Int) {
        HIGHEST(5),
        HIGH(13),
        LOW(31),
        LOWEST(47),
    }

    companion object {
        const val PROVIDER_ID = "builtin.manga.reader"
        const val READING_MODE_KEY = "reading_mode"
        const val ORIENTATION_KEY = "orientation"
        const val AUTO_SCROLL_LEVEL_MIN = 0
        const val AUTO_SCROLL_LEVEL_MAX = 6
        const val AUTO_SCROLL_LEVEL_DEFAULT = 3

        const val WEBTOON_PADDING_MIN = 0
        const val WEBTOON_PADDING_MAX = 25

        const val MILLI_CONVERSION = 100

        val AUTO_SCROLL_SPEED_RANGE = AUTO_SCROLL_LEVEL_MIN..AUTO_SCROLL_LEVEL_MAX

        val AutoScrollLevelLabels = listOf(
            MR.strings.auto_scroll_speed_slowest,
            MR.strings.auto_scroll_speed_slower,
            MR.strings.auto_scroll_speed_slow,
            MR.strings.double_tap_anim_speed_normal,
            MR.strings.double_tap_anim_speed_fast,
            MR.strings.auto_scroll_speed_faster,
            MR.strings.auto_scroll_speed_fastest,
        )

        val TapZones = listOf(
            MR.strings.label_default,
            MR.strings.l_nav,
            MR.strings.kindlish_nav,
            MR.strings.edge_nav,
            MR.strings.right_and_left_nav,
            MR.strings.disabled_nav,
        )

        val ImageScaleType = listOf(
            MR.strings.scale_type_fit_screen,
            MR.strings.scale_type_stretch,
            MR.strings.scale_type_fit_width,
            MR.strings.scale_type_fit_height,
            MR.strings.scale_type_original_size,
            MR.strings.scale_type_smart_fit,
        )

        val ZoomStart = listOf(
            MR.strings.zoom_start_automatic,
            MR.strings.zoom_start_left,
            MR.strings.zoom_start_right,
            MR.strings.zoom_start_center,
        )

        val ColorFilterMode = buildList {
            addAll(
                listOf(
                    MR.strings.label_default to BlendMode.SrcOver,
                    MR.strings.filter_mode_multiply to BlendMode.Modulate,
                    MR.strings.filter_mode_screen to BlendMode.Screen,
                ),
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                addAll(
                    listOf(
                        MR.strings.filter_mode_overlay to BlendMode.Overlay,
                        MR.strings.filter_mode_lighten to BlendMode.Lighten,
                        MR.strings.filter_mode_darken to BlendMode.Darken,
                    ),
                )
            }
        }
    }
}
