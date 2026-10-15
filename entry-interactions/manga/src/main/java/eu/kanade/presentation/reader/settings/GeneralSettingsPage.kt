package eu.kanade.presentation.reader.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import eu.kanade.tachiyomi.ui.reader.hasDisplayCutout
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.updateEntry
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.SliderRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

private val themes = listOf(
    MR.strings.black_background to 1,
    MR.strings.gray_background to 2,
    MR.strings.white_background to 0,
    MR.strings.automatic_background to 3,
)

private val flashColors = listOf(
    MR.strings.pref_flash_style_black to MangaReaderSettings.FlashColor.BLACK,
    MR.strings.pref_flash_style_white to MangaReaderSettings.FlashColor.WHITE,
    MR.strings.pref_flash_style_white_black to MangaReaderSettings.FlashColor.WHITE_BLACK,
)

@Composable
internal fun ColumnScope.GeneralPage(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()

    val readerTheme by settings.readerTheme.state.collectAsState()

    val flashPageState by settings.flashOnPageChange.state.collectAsState()

    val flashMillis by settings.flashDurationMillis.state.collectAsState()

    val flashInterval by settings.flashPageInterval.state.collectAsState()

    val flashColor by settings.flashColor.state.collectAsState()

    ChipsRow(title = stringResource(MR.strings.pref_reader_theme)) {
        themes.forEach { (labelRes, value) ->
            FilterChip(
                selected = readerTheme.effectiveValue == value,
                onClick = { scope.launch { settings.readerTheme.updateEntry(value) } },
                label = stringResource(labelRes),
            )
        }
    }

    BindingToggleRow(
        label = stringResource(MR.strings.pref_show_page_number),
        binding = settings.showPageNumber,
    )

    val verticalNavigatorModes by settings.verticalNavigator.state.collectAsState()

    ChipsRow(title = stringResource(MR.strings.pref_vertical_navigator)) {
        ReadingMode.entries.filter { it != ReadingMode.DEFAULT }.forEach { mode ->
            FilterChip(
                selected = verticalNavigatorModes.effectiveValue.contains(mode),
                onClick = {
                    val newModes = if (verticalNavigatorModes.effectiveValue.contains(mode)) {
                        verticalNavigatorModes.effectiveValue - mode
                    } else {
                        verticalNavigatorModes.effectiveValue + mode
                    }
                    scope.launch { settings.verticalNavigator.updateEntry(newModes) }
                },
                label = stringResource(mode.stringRes),
            )
        }
    }

    if (verticalNavigatorModes.effectiveValue.isNotEmpty()) {
        val verticalNavigatorHeight by settings.verticalNavigatorHeight.state.collectAsState()

        BindingToggleRow(
            label = stringResource(MR.strings.pref_webtoon_vertical_navigator_on_left),
            binding = settings.verticalNavigatorOnLeft,
        )

        SliderRow(
            title = stringResource(MR.strings.pref_vertical_navigator_height),
            value = verticalNavigatorHeight.effectiveValue,
            valueRange = 65..100,
            steps = 6,
            onValueChange = { value -> scope.launch { settings.verticalNavigatorHeight.updateEntry(value) } },
        )
    }

    BindingToggleRow(
        label = stringResource(MR.strings.pref_reader_show_status_bar),
        binding = settings.showStatusBar,
    )

    BindingToggleRow(
        label = stringResource(MR.strings.pref_reader_show_navigation_bar),
        binding = settings.showNavigationBar,
    )

    if (LocalActivity.current?.hasDisplayCutout() == true) {
        BindingToggleRow(
            label = stringResource(MR.strings.pref_cutout_short),
            binding = settings.drawUnderCutout,
        )
    }

    BindingToggleRow(
        label = stringResource(MR.strings.pref_keep_screen_on),
        binding = settings.keepScreenOn,
    )

    BindingToggleRow(
        label = stringResource(MR.strings.pref_read_with_long_tap),
        binding = settings.readWithLongTap,
    )

    val chapterTransitionMode by settings.chapterTransition.state.collectAsState()

    ChipsRow(title = stringResource(MR.strings.pref_chapter_transition)) {
        ChapterTransitionMode.entries.forEach { mode ->
            FilterChip(
                selected = chapterTransitionMode.effectiveValue == mode,
                onClick = { scope.launch { settings.chapterTransition.updateEntry(mode) } },
                label = stringResource(mode.titleRes),
            )
        }
    }

    BindingToggleRow(
        label = stringResource(MR.strings.pref_page_transitions),
        binding = settings.pageTransitions,
    )

    BindingToggleRow(
        label = stringResource(MR.strings.pref_flash_page),
        binding = settings.flashOnPageChange,
    )
    if (flashPageState.effectiveValue) {
        SliderRow(
            value = flashMillis.effectiveValue / MangaReaderSettings.MILLI_CONVERSION,
            valueRange = 1..15,
            title = stringResource(MR.strings.pref_flash_duration),
            valueText = stringResource(MR.strings.pref_flash_duration_summary, flashMillis.effectiveValue),
            onValueChange = {
                scope.launch {
                    settings.flashDurationMillis.updateEntry(it * MangaReaderSettings.MILLI_CONVERSION)
                }
            },
        )
        SliderRow(
            value = flashInterval.effectiveValue,
            valueRange = 1..10,
            title = stringResource(MR.strings.pref_flash_page_interval),
            valueText = pluralStringResource(
                MR.plurals.pref_pages,
                flashInterval.effectiveValue,
                flashInterval.effectiveValue,
            ),
            onValueChange = { value ->
                scope.launch { settings.flashPageInterval.updateEntry(value) }
            },
        )
        ChipsRow(title = stringResource(MR.strings.pref_flash_with)) {
            flashColors.forEach { (labelRes, value) ->
                FilterChip(
                    selected = flashColor.effectiveValue == value,
                    onClick = { scope.launch { settings.flashColor.updateEntry(value) } },
                    label = stringResource(labelRes),
                )
            }
        }
    }
}
