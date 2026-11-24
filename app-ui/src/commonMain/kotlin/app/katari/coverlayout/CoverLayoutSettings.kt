package app.katari.coverlayout

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.layout.CoverSizing
import app.katari.presentation.kit.layout.labelRes
import app.katari.presentation.kit.list.RowGroup
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.SliderRow
import app.katari.presentation.kit.list.ToggleRow
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Whether a source keeps a layout of its own rather than Browse's, offered where the layout of one source shows, such as
 * its catalog or a feed of it. [setting] is the source's own display mode, which syncs with the choice.
 */
class SourceOwnLayout(
    val sourceName: String,
    val own: Boolean,
    val onOwnChange: (Boolean) -> Unit,
    val setting: Preference<*>,
)

/**
 * How a screen of covers lays them out, the same wherever covers show: the display mode as drawings of it, the cover
 * size with how many covers fit per row in the grid modes, and, for one source, whether it keeps its own layout. The
 * settings rows name the preference each change is stored in, for its sync choice.
 */
@Composable
fun CoverLayoutSettings(
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    sizing: CoverSizing,
    displayModeSetting: Preference<*>,
    coverSizeSetting: Preference<*>,
    ownLayout: SourceOwnLayout? = null,
) {
    Column {
        DisplayModeChooser(
            selectedMode = displayMode,
            onModeSelected = onDisplayModeChange,
            setting = displayModeSetting,
        )
        if (displayMode.isCoverGrid) {
            RowGroupGap()
            RowGroup {
                row {
                    SliderRow(
                        title = stringResource(MR.strings.pref_library_cover_size),
                        value = sizing.size.ordinal,
                        valueRange = CoverSize.entries.indices,
                        onValueChange = { sizing.resize(CoverSize.entries[it]) },
                        valueText = coverSizeText(sizing),
                        setting = coverSizeSetting,
                    )
                }
            }
        }
        if (ownLayout != null) {
            RowGroupGap()
            RowGroup {
                row {
                    ToggleRow(
                        title = stringResource(MR.strings.cover_layout_own_title, ownLayout.sourceName),
                        subtitle = stringResource(
                            if (ownLayout.own) MR.strings.cover_layout_own_on else MR.strings.cover_layout_own_off,
                        ),
                        checked = ownLayout.own,
                        onCheckedChange = ownLayout.onOwnChange,
                        setting = ownLayout.setting,
                    )
                }
            }
        }
    }
}

// The size, and how many covers fit per row once the grid has laid them out
@Composable
private fun coverSizeText(sizing: CoverSizing): String {
    val size = stringResource(sizing.size.labelRes)
    if (sizing.columns == 0) return size
    return pluralStringResource(MR.plurals.cover_size_per_row, sizing.columns, size, sizing.columns)
}
