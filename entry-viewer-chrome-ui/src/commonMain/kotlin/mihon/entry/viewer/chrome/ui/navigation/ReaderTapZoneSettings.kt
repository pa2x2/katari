package mihon.entry.viewer.chrome.ui.navigation

import androidx.compose.runtime.Composable
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.list.ChipsRow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shared tap-zone controls for manga and BOOK, with storage owned by each settings provider. */
@Composable
fun ReaderTapZoneSettings(selected: Int, onSelect: (Int) -> Unit, inversion: Int, onInvert: (Int) -> Unit) {
    ChipsRow(stringResource(MR.strings.pref_viewer_nav)) {
        readerTapZoneLabels.forEachIndexed { index, label ->
            FilterChip(label = stringResource(label), selected = selected == index, onClick = { onSelect(index) })
        }
    }
    if (selected != 5) {
        ChipsRow(stringResource(MR.strings.pref_read_with_tapping_inverted)) {
            readerTapInversionLabels.forEachIndexed { index, label ->
                FilterChip(label = stringResource(label), selected = inversion == index, onClick = { onInvert(index) })
            }
        }
    }
}

val readerTapInversionLabels = listOf(
    MR.strings.tapping_inverted_none,
    MR.strings.tapping_inverted_horizontal,
    MR.strings.tapping_inverted_vertical,
    MR.strings.tapping_inverted_both,
)

val readerTapZoneLabels = listOf(
    MR.strings.label_default,
    MR.strings.l_nav,
    MR.strings.kindlish_nav,
    MR.strings.edge_nav,
    MR.strings.right_and_left_nav,
    MR.strings.disabled_nav,
)
