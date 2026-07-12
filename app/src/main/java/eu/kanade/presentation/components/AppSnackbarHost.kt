package eu.kanade.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

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
            action = snackbarData.visuals.actionLabel?.let { actionLabel ->
                {
                    TextButton(onClick = snackbarData::performAction) {
                        Text(text = actionLabel)
                    }
                }
            },
            dismissAction = if (snackbarData.visuals.duration == SnackbarDuration.Indefinite) {
                {
                    IconButton(onClick = snackbarData::dismiss) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
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
