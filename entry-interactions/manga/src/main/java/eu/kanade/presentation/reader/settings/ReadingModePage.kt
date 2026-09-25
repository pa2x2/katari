package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.updateEntry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.components.SliderItem
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    HeadingItem(MR.strings.pref_category_for_this_series)
    val readingMode by screenModel.readingModeFlow.collectAsState()
    SettingsChipRow(MR.strings.pref_category_reading_mode) {
        ReadingMode.entries.map {
            FilterChip(
                selected = it == readingMode,
                onClick = { screenModel.onChangeReadingMode(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val orientation by screenModel.orientationFlow.collectAsState()
    SettingsChipRow(MR.strings.rotation_type) {
        ReaderOrientation.entries.map {
            FilterChip(
                selected = it == orientation,
                onClick = { screenModel.onChangeOrientation(it) },
                label = { Text(stringResource(it.stringRes)) },
            )
        }
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
    } else {
        PagerViewerSettings(screenModel)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()
    HeadingItem(MR.strings.pager_viewer)

    val navigationMode by settings.pagerNavigationMode.state.collectAsState()
    val pagerNavInverted by settings.pagerNavigationInverted.state.collectAsState()
    TapZonesItems(
        selected = navigationMode.effectiveValue,
        onSelect = { selected -> scope.launch { settings.pagerNavigationMode.updateEntry(selected) } },
        invertMode = pagerNavInverted.effectiveValue,
        onSelectInvertMode = { mode -> scope.launch { settings.pagerNavigationInverted.updateEntry(mode) } },
    )

    val imageScaleType by settings.imageScaleType.state.collectAsState()
    SettingsChipRow(MR.strings.pref_image_scale_type) {
        MangaReaderSettings.ImageScaleType.mapIndexed { index, it ->
            FilterChip(
                selected = imageScaleType.effectiveValue == index + 1,
                onClick = { scope.launch { settings.imageScaleType.updateEntry(index + 1) } },
                label = { Text(stringResource(it)) },
            )
        }
    }

    val zoomStart by settings.zoomStart.state.collectAsState()
    SettingsChipRow(MR.strings.pref_zoom_start) {
        MangaReaderSettings.ZoomStart.mapIndexed { index, it ->
            FilterChip(
                selected = zoomStart.effectiveValue == index + 1,
                onClick = { scope.launch { settings.zoomStart.updateEntry(index + 1) } },
                label = { Text(stringResource(it)) },
            )
        }
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        binding = settings.cropBorders,
    )

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_landscape_zoom),
        binding = settings.landscapeZoom,
    )

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_navigate_pan),
        binding = settings.navigateToPan,
    )

    val dualPageSplitPaged by settings.dualPageSplitPaged.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_dual_page_split),
        binding = settings.dualPageSplitPaged,
    )

    if (dualPageSplitPaged.effectiveValue) {
        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_invert),
            binding = settings.dualPageInvertPaged,
        )
    }

    val dualPageRotateToFit by settings.dualPageRotateToFit.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_page_rotate),
        binding = settings.dualPageRotateToFit,
    )

    if (dualPageRotateToFit.effectiveValue) {
        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate_invert),
            binding = settings.dualPageRotateToFitInvert,
        )
    }
}

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    HeadingItem(MR.strings.webtoon_viewer)

    val navigationModeWebtoon by settings.webtoonNavigationMode.state.collectAsState()
    val webtoonNavInverted by settings.webtoonNavigationInverted.state.collectAsState()
    TapZonesItems(
        selected = navigationModeWebtoon.effectiveValue,
        onSelect = { selected -> scope.launch { settings.webtoonNavigationMode.updateEntry(selected) } },
        invertMode = webtoonNavInverted.effectiveValue,
        onSelectInvertMode = { mode -> scope.launch { settings.webtoonNavigationInverted.updateEntry(mode) } },
    )

    val webtoonSidePadding by settings.webtoonSidePadding.state.collectAsState()
    SliderItem(
        value = webtoonSidePadding.effectiveValue,
        valueRange = MangaReaderSettings.WEBTOON_PADDING_MIN..MangaReaderSettings.WEBTOON_PADDING_MAX,
        label = stringResource(MR.strings.pref_webtoon_side_padding),
        valueString = numberFormat.format(webtoonSidePadding.effectiveValue / 100f),
        onChange = { value -> scope.launch { settings.webtoonSidePadding.updateEntry(value) } },
        pillColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_crop_borders),
        binding = settings.cropBordersWebtoon,
    )

    val dualPageSplitWebtoon by settings.dualPageSplitWebtoon.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_dual_page_split),
        binding = settings.dualPageSplitWebtoon,
    )

    if (dualPageSplitWebtoon.effectiveValue) {
        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_dual_page_invert),
            binding = settings.dualPageInvertWebtoon,
        )
    }

    val dualPageRotateToFitWebtoon by settings.dualPageRotateToFitWebtoon.state.collectAsState()
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_page_rotate),
        binding = settings.dualPageRotateToFitWebtoon,
    )

    if (dualPageRotateToFitWebtoon.effectiveValue) {
        BindingCheckboxItem(
            label = stringResource(MR.strings.pref_page_rotate_invert),
            binding = settings.dualPageRotateToFitInvertWebtoon,
        )
    }

    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_double_tap_zoom),
        binding = settings.webtoonDoubleTapZoom,
    )
    BindingCheckboxItem(
        label = stringResource(MR.strings.pref_webtoon_disable_zoom_out),
        binding = settings.webtoonDisableZoomOut,
    )
}

@Composable
private fun ColumnScope.TapZonesItems(
    selected: Int,
    onSelect: (Int) -> Unit,
    invertMode: MangaReaderSettings.TappingInvertMode,
    onSelectInvertMode: (MangaReaderSettings.TappingInvertMode) -> Unit,
) {
    tachiyomi.presentation.core.components.reader.navigation.ReaderTapZoneSettings(
        selected,
        onSelect,
        invertMode.ordinal,
        { onSelectInvertMode(MangaReaderSettings.TappingInvertMode.entries[it]) },
    )
}
