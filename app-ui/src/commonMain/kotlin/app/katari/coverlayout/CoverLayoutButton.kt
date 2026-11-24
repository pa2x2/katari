package app.katari.coverlayout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredroundedfilled.ViewList
import app.katari.icons.materialsymbols.roundedfilled.ViewCompact
import app.katari.icons.materialsymbols.roundedfilled.ViewModule
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.sheet.Popover
import app.katari.presentation.kit.sheet.PopoverAnchor
import app.katari.presentation.kit.sheet.SheetHeader
import app.katari.presentation.kit.sheet.popoverAnchor
import app.katari.presentation.kit.sheet.rememberPopoverAnchor
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The display button of a screen of covers in its app bar, drawn as [displayMode], which opens the Display panel
 * ([settings], a [CoverLayoutSettings]) beside it: a card from medium windows on, so the covers behind change as the
 * person picks, and a sheet on a phone.
 */
@Composable
fun rememberCoverLayoutAction(displayMode: LibraryDisplayMode, settings: @Composable () -> Unit): AppBar.Action {
    var open by rememberSaveable { mutableStateOf(false) }
    val anchor = rememberPopoverAnchor()
    return AppBar.Action(
        title = stringResource(MR.strings.action_display),
        icon = displayMode.icon,
        onClick = { open = !open },
        anchor = anchor,
        menu = { if (open) CoverLayoutPopover(anchor, onDismissRequest = { open = false }, settings) },
    )
}

/** The display button of [rememberCoverLayoutAction] where covers show without an app bar, such as a feed's bar. */
@Composable
fun CoverLayoutButton(displayMode: LibraryDisplayMode, settings: @Composable () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val anchor = rememberPopoverAnchor()
    IconButton(
        icon = displayMode.icon,
        contentDescription = stringResource(MR.strings.action_display),
        onClick = { open = !open },
        modifier = Modifier.popoverAnchor(anchor),
    )
    if (open) CoverLayoutPopover(anchor, onDismissRequest = { open = false }, settings)
}

@Composable
private fun CoverLayoutPopover(anchor: PopoverAnchor, onDismissRequest: () -> Unit, settings: @Composable () -> Unit) {
    Popover(anchor = anchor, onDismissRequest = onDismissRequest) {
        Column {
            SheetHeader(title = stringResource(MR.strings.action_display))
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = KatariTheme.spacing.m),
            ) {
                settings()
            }
        }
    }
}

private val LibraryDisplayMode.icon: ImageVector
    get() = when (this) {
        LibraryDisplayMode.List,
        LibraryDisplayMode.ComfortableList,
        -> MaterialSymbols.AutoMirroredRoundedFilled.ViewList
        LibraryDisplayMode.ComfortableGrid -> MaterialSymbols.RoundedFilled.ViewModule
        LibraryDisplayMode.CompactGrid,
        LibraryDisplayMode.CoverOnlyGrid,
        -> MaterialSymbols.RoundedFilled.ViewCompact
    }
