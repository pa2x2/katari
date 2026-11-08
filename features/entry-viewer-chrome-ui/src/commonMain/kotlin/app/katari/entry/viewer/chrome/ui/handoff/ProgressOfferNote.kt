package app.katari.entry.viewer.chrome.ui.handoff

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Devices
import app.katari.presentation.kit.feedback.OfferNote
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A reader's or the player's offer to go where another device got to (D053), such as "Fedora desktop is further" over
 * "Chapter 12, page 18". [place] says where, in the reader's own terms.
 */
@Composable
fun ProgressOfferNote(
    deviceName: String?,
    place: String,
    onGoThere: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OfferNote(
        heading = deviceName
            ?.let { stringResource(MR.strings.sync_handoff_further, it) }
            ?: stringResource(MR.strings.sync_handoff_further_unknown),
        text = place,
        actionLabel = stringResource(MR.strings.sync_action_go_there),
        onAction = onGoThere,
        onDismiss = onDismiss,
        modifier = modifier,
        icon = MaterialSymbols.Rounded.Devices,
    )
}
