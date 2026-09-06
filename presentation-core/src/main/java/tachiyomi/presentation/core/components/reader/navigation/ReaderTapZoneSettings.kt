package tachiyomi.presentation.core.components.reader.navigation

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SettingsChipRow
import tachiyomi.presentation.core.i18n.stringResource

/** Shared tap-zone controls for manga and BOOK, with storage owned by each settings provider. */
@Composable
fun ReaderTapZoneSettings(selected: Int, onSelect: (Int) -> Unit, inversion: Int, onInvert: (Int) -> Unit) {
    SettingsChipRow(MR.strings.pref_viewer_nav) {
        readerTapZoneLabels.forEachIndexed { index, label ->
            FilterChip(selected == index, { onSelect(index) }, label = { Text(stringResource(label)) })
        }
    }
    if (selected != 5) {
        SettingsChipRow(MR.strings.pref_read_with_tapping_inverted) {
            readerTapInversionLabels.forEachIndexed { index, label ->
                FilterChip(inversion == index, { onInvert(index) }, label = { Text(stringResource(label)) })
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
