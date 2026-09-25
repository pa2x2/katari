package eu.kanade.presentation.reader.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
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

    SettingsChipRow(MR.strings.pref_reader_theme) {
        themes.forEach { (labelRes, value) ->
            FilterChip(
                selected = readerTheme.effectiveValue == value,
                onClick = { scope.launch { settings.readerTheme.updateEntry(value) } },
                label = { Text(stringResource(labelRes)) },
            )
        }
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_show_page_number),
        binding = settings.showPageNumber,
    )

    val verticalNavigatorModes by settings.verticalNavigator.state.collectAsState()

    SettingsChipRow(MR.strings.pref_vertical_navigator) {
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
                label = { Text(stringResource(mode.stringRes)) },
            )
        }
    }

    if (verticalNavigatorModes.effectiveValue.isNotEmpty()) {
        val verticalNavigatorHeight by settings.verticalNavigatorHeight.state.collectAsState()

        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_webtoon_vertical_navigator_on_left),
            binding = settings.verticalNavigatorOnLeft,
        )

        SliderItem(
            label = stringResource(MR.strings.pref_vertical_navigator_height),
            value = verticalNavigatorHeight.effectiveValue,
            valueRange = 65..100,
            steps = 6,
            onChange = { value -> scope.launch { settings.verticalNavigatorHeight.updateEntry(value) } },
        )
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_fullscreen),
        binding = settings.fullscreen,
    )

    val isFullscreen by settings.fullscreen.state.collectAsState()
    if (LocalActivity.current?.hasDisplayCutout() == true && isFullscreen.effectiveValue) {
        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_cutout_short),
            binding = settings.drawUnderCutout,
        )
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_keep_screen_on),
        binding = settings.keepScreenOn,
    )

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_read_with_long_tap),
        binding = settings.readWithLongTap,
    )

    val chapterTransitionMode by settings.chapterTransition.state.collectAsState()

    SettingsChipRow(MR.strings.pref_chapter_transition) {
        ChapterTransitionMode.entries.forEach { mode ->
            FilterChip(
                selected = chapterTransitionMode.effectiveValue == mode,
                onClick = { scope.launch { settings.chapterTransition.updateEntry(mode) } },
                label = { Text(stringResource(mode.titleRes)) },
            )
        }
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_page_transitions),
        binding = settings.pageTransitions,
    )

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_flash_page),
        binding = settings.flashOnPageChange,
    )
    if (flashPageState.effectiveValue) {
        SliderItem(
            value = flashMillis.effectiveValue / MangaReaderSettings.MILLI_CONVERSION,
            valueRange = 1..15,
            label = stringResource(MR.strings.pref_flash_duration),
            valueString = stringResource(MR.strings.pref_flash_duration_summary, flashMillis.effectiveValue),
            onChange = {
                scope.launch {
                    settings.flashDurationMillis.updateEntry(it * MangaReaderSettings.MILLI_CONVERSION)
                }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SliderItem(
            value = flashInterval.effectiveValue,
            valueRange = 1..10,
            label = stringResource(MR.strings.pref_flash_page_interval),
            valueString = pluralStringResource(
                MR.plurals.pref_pages,
                flashInterval.effectiveValue,
                flashInterval.effectiveValue,
            ),
            onChange = { value ->
                scope.launch { settings.flashPageInterval.updateEntry(value) }
            },
            pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        SettingsChipRow(MR.strings.pref_flash_with) {
            flashColors.forEach { (labelRes, value) ->
                FilterChip(
                    selected = flashColor.effectiveValue == value,
                    onClick = { scope.launch { settings.flashColor.updateEntry(value) } },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }
    }
}
