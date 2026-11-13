package app.katari.entry.viewer.chrome.ui.tap

import androidx.compose.runtime.Composable
import app.katari.platform.presentation.platformPresentation
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.list.ChipsRow
import tachiyomi.core.common.preference.Preference
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Shared tap-zone controls for manga and BOOK, with storage owned by each settings provider: [zonesSetting] and
 * [inversionSetting] are the settings they pick, whose marks show after their headers.
 */
@Composable
fun ReaderTapZoneSettings(
    selected: Int,
    onSelect: (Int) -> Unit,
    inversion: Int,
    onInvert: (Int) -> Unit,
    zonesSetting: Preference<*>,
    inversionSetting: Preference<*>,
) {
    ChipsRow(stringResource(platformPresentation.pageZonesLabel), setting = zonesSetting) {
        readerTapZoneLabels.forEachIndexed { index, label ->
            FilterChip(label = stringResource(label), selected = selected == index, onClick = { onSelect(index) })
        }
    }
    if (selected != 5) {
        ChipsRow(stringResource(platformPresentation.invertPageZonesLabel), setting = inversionSetting) {
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
