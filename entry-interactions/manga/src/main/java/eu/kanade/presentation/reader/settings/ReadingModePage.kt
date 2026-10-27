package eu.kanade.presentation.reader.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.viewer.pager.VerticalPagerViewer
import eu.kanade.tachiyomi.ui.reader.viewer.webtoon.WebtoonViewer
import kotlinx.coroutines.launch
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReaderPageLayout
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.updateEntry
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import mihon.presentation.kit.list.RowGroup
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.SliderRow
import mihon.presentation.kit.window.currentWindowLayout
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import java.text.NumberFormat

@Composable
internal fun ColumnScope.ReadingModePage(screenModel: ReaderSettingsScreenModel) {
    RowGroupHeader(stringResource(MR.strings.pref_category_for_this_series))
    val readingMode by screenModel.readingModeFlow.collectAsState()
    ChipsRow(title = stringResource(MR.strings.pref_category_reading_mode)) {
        ReadingMode.entries.map {
            FilterChip(
                selected = it == readingMode,
                onClick = { screenModel.onChangeReadingMode(it) },
                label = stringResource(it.stringRes),
            )
        }
    }

    val orientation by screenModel.orientationFlow.collectAsState()
    ChipsRow(
        title = stringResource(MR.strings.rotation_type),
        supportingText = stringResource(MR.strings.rotation_not_applied)
            .takeUnless { currentWindowLayout().honoursOrientationRequests },
    ) {
        ReaderOrientation.entries.map {
            FilterChip(
                selected = it == orientation,
                onClick = { screenModel.onChangeOrientation(it) },
                label = stringResource(it.stringRes),
            )
        }
    }

    val viewer by screenModel.viewerFlow.collectAsState()
    if (viewer is WebtoonViewer) {
        WebtoonViewerSettings(screenModel)
    } else {
        PagerViewerSettings(screenModel, showsPageLayout = viewer !is VerticalPagerViewer)
    }
}

@Composable
private fun ColumnScope.PagerViewerSettings(screenModel: ReaderSettingsScreenModel, showsPageLayout: Boolean) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()
    RowGroupHeader(stringResource(MR.strings.pager_viewer))

    if (showsPageLayout) {
        val pageLayout by settings.pagerPageLayout.state.collectAsState()
        ChipsRow(title = stringResource(MR.strings.pref_reader_pages)) {
            ReaderPageLayout.entries.forEach { value ->
                FilterChip(
                    selected = pageLayout.effectiveValue == value,
                    onClick = { scope.launch { settings.pagerPageLayout.updateEntry(value) } },
                    label = stringResource(value.stringRes),
                )
            }
        }
    }

    val navigationMode by settings.pagerNavigationMode.state.collectAsState()
    val pagerNavInverted by settings.pagerNavigationInverted.state.collectAsState()
    TapZonesItems(
        selected = navigationMode.effectiveValue,
        onSelect = { selected -> scope.launch { settings.pagerNavigationMode.updateEntry(selected) } },
        invertMode = pagerNavInverted.effectiveValue,
        onSelectInvertMode = { mode -> scope.launch { settings.pagerNavigationInverted.updateEntry(mode) } },
    )

    val imageScaleType by settings.imageScaleType.state.collectAsState()
    ChipsRow(title = stringResource(MR.strings.pref_image_scale_type)) {
        MangaReaderSettings.ImageScaleType.mapIndexed { index, it ->
            FilterChip(
                selected = imageScaleType.effectiveValue == index + 1,
                onClick = { scope.launch { settings.imageScaleType.updateEntry(index + 1) } },
                label = stringResource(it),
            )
        }
    }

    val zoomStart by settings.zoomStart.state.collectAsState()
    ChipsRow(title = stringResource(MR.strings.pref_zoom_start)) {
        MangaReaderSettings.ZoomStart.mapIndexed { index, it ->
            FilterChip(
                selected = zoomStart.effectiveValue == index + 1,
                onClick = { scope.launch { settings.zoomStart.updateEntry(index + 1) } },
                label = stringResource(it),
            )
        }
    }

    val dualPageSplitPaged by settings.dualPageSplitPaged.state.collectAsState()
    val dualPageRotateToFit by settings.dualPageRotateToFit.state.collectAsState()
    RowGroup {
        row { BindingToggleRow(stringResource(MR.strings.pref_crop_borders), settings.cropBorders) }
        row { BindingToggleRow(stringResource(MR.strings.pref_landscape_zoom), settings.landscapeZoom) }
        row { BindingToggleRow(stringResource(MR.strings.pref_navigate_pan), settings.navigateToPan) }
        row { BindingToggleRow(stringResource(MR.strings.pref_dual_page_split), settings.dualPageSplitPaged) }
        if (dualPageSplitPaged.effectiveValue) {
            row { BindingToggleRow(stringResource(MR.strings.pref_dual_page_invert), settings.dualPageInvertPaged) }
        }
        row { BindingToggleRow(stringResource(MR.strings.pref_page_rotate), settings.dualPageRotateToFit) }
        if (dualPageRotateToFit.effectiveValue) {
            row {
                BindingToggleRow(stringResource(MR.strings.pref_page_rotate_invert), settings.dualPageRotateToFitInvert)
            }
        }
    }
}

@Composable
private fun ColumnScope.WebtoonViewerSettings(screenModel: ReaderSettingsScreenModel) {
    val settings = screenModel.settings
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getPercentInstance() }

    RowGroupHeader(stringResource(MR.strings.webtoon_viewer))

    val navigationModeWebtoon by settings.webtoonNavigationMode.state.collectAsState()
    val webtoonNavInverted by settings.webtoonNavigationInverted.state.collectAsState()
    TapZonesItems(
        selected = navigationModeWebtoon.effectiveValue,
        onSelect = { selected -> scope.launch { settings.webtoonNavigationMode.updateEntry(selected) } },
        invertMode = webtoonNavInverted.effectiveValue,
        onSelectInvertMode = { mode -> scope.launch { settings.webtoonNavigationInverted.updateEntry(mode) } },
    )

    val webtoonSidePadding by settings.webtoonSidePadding.state.collectAsState()
    val dualPageSplitWebtoon by settings.dualPageSplitWebtoon.state.collectAsState()
    val dualPageRotateToFitWebtoon by settings.dualPageRotateToFitWebtoon.state.collectAsState()
    RowGroup {
        row {
            SliderRow(
                value = webtoonSidePadding.effectiveValue,
                valueRange = MangaReaderSettings.WEBTOON_PADDING_MIN..MangaReaderSettings.WEBTOON_PADDING_MAX,
                title = stringResource(MR.strings.pref_webtoon_side_padding),
                valueText = numberFormat.format(webtoonSidePadding.effectiveValue / 100f),
                onValueChange = { value -> scope.launch { settings.webtoonSidePadding.updateEntry(value) } },
            )
        }
        row { BindingToggleRow(stringResource(MR.strings.pref_crop_borders), settings.cropBordersWebtoon) }
        row { BindingToggleRow(stringResource(MR.strings.pref_dual_page_split), settings.dualPageSplitWebtoon) }
        if (dualPageSplitWebtoon.effectiveValue) {
            row { BindingToggleRow(stringResource(MR.strings.pref_dual_page_invert), settings.dualPageInvertWebtoon) }
        }
        row { BindingToggleRow(stringResource(MR.strings.pref_page_rotate), settings.dualPageRotateToFitWebtoon) }
        if (dualPageRotateToFitWebtoon.effectiveValue) {
            row {
                BindingToggleRow(
                    stringResource(MR.strings.pref_page_rotate_invert),
                    settings.dualPageRotateToFitInvertWebtoon,
                )
            }
        }
        row { BindingToggleRow(stringResource(MR.strings.pref_double_tap_zoom), settings.webtoonDoubleTapZoom) }
        row {
            BindingToggleRow(stringResource(MR.strings.pref_webtoon_disable_zoom_out), settings.webtoonDisableZoomOut)
        }
    }
}

@Composable
private fun ColumnScope.TapZonesItems(
    selected: Int,
    onSelect: (Int) -> Unit,
    invertMode: MangaReaderSettings.TappingInvertMode,
    onSelectInvertMode: (MangaReaderSettings.TappingInvertMode) -> Unit,
) {
    mihon.entry.viewer.chrome.ui.navigation.ReaderTapZoneSettings(
        selected,
        onSelect,
        invertMode.ordinal,
        { onSelectInvertMode(MangaReaderSettings.TappingInvertMode.entries[it]) },
    )
}
