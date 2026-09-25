package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.hasDisplayCutout
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.asProfilePreference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.NumberFormat
import tachiyomi.core.common.preference.Preference as CorePreference

object SettingsMangaReaderScreen : AppEntryViewerSettingsScreenProjection() {

    override val surfaceId: String = MangaReaderSettings.PROVIDER_ID

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_manga_reader

    @Composable
    override fun getSurfacePreferences(): List<Preference> {
        val settings = remember { Injekt.get<MangaReaderSettings>() }
        val settingBinder = remember { Injekt.get<ViewerSettingBinder>() }
        val defaultReadingMode = settings.profilePreference(settingBinder, settings.reading.readingMode)
        val defaultOrientation = settings.profilePreference(settingBinder, settings.reading.orientation)

        return listOf(
            Preference.PreferenceItem.ListPreference(
                preference = defaultReadingMode,
                entries = ReadingMode.entries.drop(1)
                    .associate { it.flagValue to stringResource(it.stringRes) },
                title = stringResource(MR.strings.pref_viewer_type),
            ),
            Preference.PreferenceItem.ListPreference(
                preference = settings.profilePreference(settingBinder, settings.display.doubleTapAnimSpeed),
                entries = mapOf(
                    1 to stringResource(MR.strings.double_tap_anim_speed_0),
                    500 to stringResource(MR.strings.double_tap_anim_speed_normal),
                    250 to stringResource(MR.strings.double_tap_anim_speed_fast),
                ),
                title = stringResource(MR.strings.pref_double_tap_anim_speed),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = settings.profilePreference(settingBinder, settings.display.showReadingMode),
                title = stringResource(MR.strings.pref_show_reading_mode),
                subtitle = stringResource(MR.strings.pref_show_reading_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = settings.profilePreference(
                    settingBinder,
                    settings.display.showNavigationOverlayOnStart,
                ),
                title = stringResource(MR.strings.pref_show_navigation_mode),
                subtitle = stringResource(MR.strings.pref_show_navigation_mode_summary),
            ),
            Preference.PreferenceItem.SwitchPreference(
                preference = settings.profilePreference(settingBinder, settings.reading.pageTransitions),
                title = stringResource(MR.strings.pref_page_transitions),
            ),
            getDisplayGroup(settings, settingBinder, defaultOrientation),
            getEInkGroup(settings, settingBinder),
            getReadingGroup(settings, settingBinder),
            getPagedGroup(settings, settingBinder),
            getWebtoonGroup(settings, settingBinder),
            getNavigationGroup(settings, settingBinder),
            getAutoScrollGroup(settings, settingBinder),
            getActionsGroup(settings, settingBinder),
        )
    }

    @Composable
    private fun getDisplayGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
        defaultOrientation: CorePreference<Int>,
    ): Preference.PreferenceGroup {
        val fullscreenPreference = settings.profilePreference(settingBinder, settings.display.fullscreen)
        val fullscreen by fullscreenPreference.collectAsState()
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_display),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = defaultOrientation,
                    entries = ReaderOrientation.entries.drop(1)
                        .associate { it.flagValue to stringResource(it.stringRes) },
                    title = stringResource(MR.strings.pref_rotation_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.display.readerTheme),
                    entries = mapOf(
                        1 to stringResource(MR.strings.black_background),
                        2 to stringResource(MR.strings.gray_background),
                        0 to stringResource(MR.strings.white_background),
                        3 to stringResource(MR.strings.automatic_background),
                    ),
                    title = stringResource(MR.strings.pref_reader_theme),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = fullscreenPreference,
                    title = stringResource(MR.strings.pref_fullscreen),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.display.drawUnderCutout),
                    title = stringResource(MR.strings.pref_cutout_short),
                    enabled = LocalView.current.hasDisplayCutout() && fullscreen,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.display.keepScreenOn),
                    title = stringResource(MR.strings.pref_keep_screen_on),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.display.showPageNumber),
                    title = stringResource(MR.strings.pref_show_page_number),
                ),
            ),
        )
    }

    @Composable
    private fun getEInkGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        val flashPageStatePreference = settings.profilePreference(settingBinder, settings.eInk.flashOnPageChange)
        val flashPageState by flashPageStatePreference.collectAsState()

        val flashMillisPref = settings.profilePreference(settingBinder, settings.eInk.flashDurationMillis)
        val flashMillis by flashMillisPref.collectAsState()

        val flashIntervalPref = settings.profilePreference(settingBinder, settings.eInk.flashPageInterval)
        val flashInterval by flashIntervalPref.collectAsState()

        val flashColorPref = settings.profilePreference(settingBinder, settings.eInk.flashColor)

        return Preference.PreferenceGroup(
            title = "E-Ink",
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = flashPageStatePreference,
                    title = stringResource(MR.strings.pref_flash_page),
                    subtitle = stringResource(MR.strings.pref_flash_page_summ),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = flashMillis / MangaReaderSettings.MILLI_CONVERSION,
                    preference = flashMillisPref,
                    valueRange = 1..15,
                    title = stringResource(MR.strings.pref_flash_duration),
                    valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis),
                    isProfileSpecific = false,
                    enabled = flashPageState,
                    onValueChanged = { flashMillisPref.set(it * MangaReaderSettings.MILLI_CONVERSION) },
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = flashInterval,
                    preference = flashIntervalPref,
                    valueRange = 1..10,
                    title = stringResource(MR.strings.pref_flash_page_interval),
                    valueString = pluralStringResource(MR.plurals.pref_pages, flashInterval, flashInterval),
                    enabled = flashPageState,
                    onValueChanged = { flashIntervalPref.set(it) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = flashColorPref,
                    entries = mapOf(
                        MangaReaderSettings.FlashColor.BLACK to
                            stringResource(MR.strings.pref_flash_style_black),
                        MangaReaderSettings.FlashColor.WHITE to
                            stringResource(MR.strings.pref_flash_style_white),
                        MangaReaderSettings.FlashColor.WHITE_BLACK
                            to stringResource(MR.strings.pref_flash_style_white_black),
                    ),
                    title = stringResource(MR.strings.pref_flash_with),
                    enabled = flashPageState,
                ),
            ),
        )
    }

    @Composable
    private fun getReadingGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_reading),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.skipRead),
                    title = stringResource(MR.strings.pref_skip_read_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.skipFiltered),
                    title = stringResource(MR.strings.pref_skip_filtered_chapters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.skipDuplicate),
                    title = stringResource(MR.strings.pref_skip_dupe_chapters),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.chapterTransition),
                    entries = ChapterTransitionMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_chapter_transition),
                ),
            ),
        )
    }

    @Composable
    private fun getPagedGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        val navModePref = settings.profilePreference(settingBinder, settings.pager.navigationMode)
        val imageScaleTypePref = settings.profilePreference(settingBinder, settings.pager.imageScaleType)
        val dualPageSplitPref = settings.profilePreference(settingBinder, settings.pager.dualPageSplit)
        val rotateToFitPref = settings.profilePreference(settingBinder, settings.pager.dualPageRotateToFit)

        val navMode by navModePref.collectAsState()
        val imageScaleType by imageScaleTypePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pager_viewer),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    entries = MangaReaderSettings.TapZones
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.navigationInverted),
                    entries = MangaReaderSettings.TappingInvertMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    enabled = navMode != 5,
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = imageScaleTypePref,
                    entries = MangaReaderSettings.ImageScaleType
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_image_scale_type),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.zoomStart),
                    entries = MangaReaderSettings.ZoomStart
                        .mapIndexed { index, it -> index + 1 to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_zoom_start),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.cropBorders),
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.landscapeZoom),
                    title = stringResource(MR.strings.pref_landscape_zoom),
                    enabled = imageScaleType == 1,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.navigateToPan),
                    title = stringResource(MR.strings.pref_navigate_pan),
                    enabled = navMode != 5,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.pager.dualPageInvert),
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    enabled = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(
                        settingBinder,
                        settings.pager.dualPageRotateToFitInvert,
                    ),
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    enabled = rotateToFit,
                ),
            ),
        )
    }

    @Composable
    private fun getWebtoonGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        val numberFormat = remember { NumberFormat.getPercentInstance() }

        val navModePref = settings.profilePreference(settingBinder, settings.webtoon.navigationMode)
        val dualPageSplitPref = settings.profilePreference(settingBinder, settings.webtoon.dualPageSplit)
        val rotateToFitPref = settings.profilePreference(settingBinder, settings.webtoon.dualPageRotateToFit)
        val webtoonSidePaddingPref =
            settings.profilePreference(settingBinder, settings.webtoon.sidePadding)

        val navMode by navModePref.collectAsState()
        val dualPageSplit by dualPageSplitPref.collectAsState()
        val rotateToFit by rotateToFitPref.collectAsState()
        val webtoonSidePadding by webtoonSidePaddingPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.webtoon_viewer),
            preferenceItems = listOf(
                Preference.PreferenceItem.ListPreference(
                    preference = navModePref,
                    entries = MangaReaderSettings.TapZones
                        .mapIndexed { index, it -> index to stringResource(it) }
                        .toMap(),
                    title = stringResource(MR.strings.pref_viewer_nav),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.navigationInverted),
                    entries = MangaReaderSettings.TappingInvertMode.entries
                        .associateWith { stringResource(it.titleRes) },
                    title = stringResource(MR.strings.pref_read_with_tapping_inverted),
                    enabled = navMode != 5,
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = webtoonSidePadding,
                    preference = webtoonSidePaddingPref,
                    valueRange = MangaReaderSettings.WEBTOON_PADDING_MIN..MangaReaderSettings.WEBTOON_PADDING_MAX,
                    title = stringResource(MR.strings.pref_webtoon_side_padding),
                    valueString = numberFormat.format(webtoonSidePadding / 100f),
                    onValueChanged = { webtoonSidePaddingPref.set(it) },
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.hideThreshold),
                    entries = mapOf(
                        MangaReaderSettings.ReaderHideThreshold.HIGHEST to
                            stringResource(MR.strings.pref_highest),
                        MangaReaderSettings.ReaderHideThreshold.HIGH to stringResource(MR.strings.pref_high),
                        MangaReaderSettings.ReaderHideThreshold.LOW to stringResource(MR.strings.pref_low),
                        MangaReaderSettings.ReaderHideThreshold.LOWEST to stringResource(
                            MR.strings.pref_lowest,
                        ),
                    ),
                    title = stringResource(MR.strings.pref_hide_threshold),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.cropBorders),
                    title = stringResource(MR.strings.pref_crop_borders),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = dualPageSplitPref,
                    title = stringResource(MR.strings.pref_dual_page_split),
                    onValueChanged = {
                        rotateToFitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.dualPageInvert),
                    title = stringResource(MR.strings.pref_dual_page_invert),
                    subtitle = stringResource(MR.strings.pref_dual_page_invert_summary),
                    enabled = dualPageSplit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = rotateToFitPref,
                    title = stringResource(MR.strings.pref_page_rotate),
                    onValueChanged = {
                        dualPageSplitPref.set(false)
                        true
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(
                        settingBinder,
                        settings.webtoon.dualPageRotateToFitInvert,
                    ),
                    title = stringResource(MR.strings.pref_page_rotate_invert),
                    enabled = rotateToFit,
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.doubleTapZoom),
                    title = stringResource(MR.strings.pref_double_tap_zoom),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.webtoon.disableZoomOut),
                    title = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
                ),
            ),
        )
    }

    @Composable
    private fun getNavigationGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        val readWithVolumeKeysPref = settings.profilePreference(settingBinder, settings.navigation.volumeKeys)
        val readWithVolumeKeys by readWithVolumeKeysPref.collectAsState()

        val verticalNavigator by settings.profilePreference(settingBinder, settings.navigation.verticalNavigator)
            .collectAsState()
        val verticalNavigatorHeightPref =
            settings.profilePreference(settingBinder, settings.navigation.verticalNavigatorHeight)
        val verticalNavigatorHeight by verticalNavigatorHeightPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_navigation),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = readWithVolumeKeysPref,
                    title = stringResource(MR.strings.pref_read_with_volume_keys),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.navigation.volumeKeysInverted),
                    title = stringResource(MR.strings.pref_read_with_volume_keys_inverted),
                    enabled = readWithVolumeKeys,
                ),
                Preference.PreferenceItem.MultiSelectListPreference(
                    preference = settings.profilePreference(settingBinder, settings.navigation.verticalNavigator),
                    entries = ReadingMode.entries.filter { it != ReadingMode.DEFAULT }
                        .associate { it to stringResource(it.stringRes) },
                    title = stringResource(MR.strings.pref_vertical_navigator),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(
                        settingBinder,
                        settings.navigation.verticalNavigatorOnLeft,
                    ),
                    title = stringResource(MR.strings.pref_webtoon_vertical_navigator_on_left),
                    enabled = verticalNavigator.isNotEmpty(),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = verticalNavigatorHeight,
                    valueRange = 65..100,
                    steps = 6,
                    title = stringResource(MR.strings.pref_vertical_navigator_height),
                    onValueChanged = { verticalNavigatorHeightPref.set(it) },
                    enabled = verticalNavigator.isNotEmpty(),
                ),
            ),
        )
    }

    @Composable
    private fun getAutoScrollGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        val autoScrollEnabledPref = settings.profilePreference(settingBinder, settings.navigation.autoScrollEnabled)
        val autoScrollEnabled by autoScrollEnabledPref.collectAsState()
        val autoScrollSpeedPref = settings.profilePreference(settingBinder, settings.navigation.autoScrollSpeed)
        val autoScrollSpeed by autoScrollSpeedPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_auto_scroll),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = autoScrollEnabledPref,
                    title = stringResource(MR.strings.pref_enable_auto_scroll),
                    subtitle = stringResource(MR.strings.pref_auto_scroll_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = autoScrollSpeed,
                    preference = autoScrollSpeedPref,
                    valueRange = MangaReaderSettings.AUTO_SCROLL_SPEED_RANGE,
                    title = stringResource(MR.strings.pref_auto_scroll_speed),
                    valueString = stringResource(MangaReaderSettings.AutoScrollLevelLabels[autoScrollSpeed]),
                    enabled = autoScrollEnabled,
                    onValueChanged = { autoScrollSpeedPref.set(it) },
                ),
            ),
        )
    }

    @Composable
    private fun getActionsGroup(
        settings: MangaReaderSettings,
        settingBinder: ViewerSettingBinder,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_reader_actions),
            preferenceItems = listOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.readWithLongTap),
                    title = stringResource(MR.strings.pref_read_with_long_tap),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = settings.profilePreference(settingBinder, settings.reading.folderPerManga),
                    title = stringResource(MR.strings.pref_create_folder_per_manga),
                    subtitle = stringResource(MR.strings.pref_create_folder_per_manga_summary),
                ),
            ),
        )
    }
}

@Composable
private fun <T> MangaReaderSettings.profilePreference(
    binder: ViewerSettingBinder,
    definition: ViewerSettingDefinition<T>,
): CorePreference<T> {
    return remember(binder, definition) { binder.bind(definition).asProfilePreference() }
}
