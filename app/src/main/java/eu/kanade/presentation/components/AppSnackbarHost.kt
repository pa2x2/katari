package eu.kanade.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Material's own `Snackbar(snackbarData)` with a Material Symbols close button that indefinite snackbars always get, so
 * it has to restate that overload's margin and action color, which the slot-based `Snackbar` leaves out.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val closeLabel = stringResource(MR.strings.action_close)

    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) { snackbarData ->
        Snackbar(
            modifier = Modifier.padding(12.dp),
            action = snackbarData.visuals.actionLabel?.let { actionLabel ->
                {
                    TextButton(
                        onClick = snackbarData::performAction,
                        colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                    ) {
                        Text(text = actionLabel)
                    }
                }
            },
            dismissAction = if (
                snackbarData.visuals.withDismissAction ||
                snackbarData.visuals.duration == SnackbarDuration.Indefinite
            ) {
                {
                    IconButton(onClick = snackbarData::dismiss) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = closeLabel,
                        )
                    }
                }
            } else {
                null
            },
        ) {
            Text(text = snackbarData.visuals.message)
        }
    }
}
