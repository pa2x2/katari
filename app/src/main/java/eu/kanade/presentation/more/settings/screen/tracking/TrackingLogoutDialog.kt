package eu.kanade.presentation.more.settings.screen.tracking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import eu.kanade.tachiyomi.util.system.toast
import mihon.entry.interactions.tracking.EntryTrackingAccount
import mihon.entry.interactions.tracking.EntryTrackingAccountOperationResult
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.presentation.kit.dialog.ConfirmDialog
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TrackingLogoutDialog(
    account: EntryTrackingAccount,
    trackingFeature: EntryTrackingFeature,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ConfirmDialog(
        title = stringResource(MR.strings.logout_title, account.service.name),
        text = null,
        confirmLabel = stringResource(MR.strings.logout),
        onConfirm = {
            scope.launchIO {
                when (val result = trackingFeature.logout(account.service.id)) {
                    EntryTrackingAccountOperationResult.Completed -> withUIContext {
                        onDismissRequest()
                        context.toast(MR.strings.logout_success)
                    }
                    is EntryTrackingAccountOperationResult.Failed -> withUIContext {
                        context.toast(result.cause.message.toString())
                    }
                    is EntryTrackingAccountOperationResult.ExternalAuthorization,
                    is EntryTrackingAccountOperationResult.Unavailable,
                    -> Unit
                }
            }
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
    )
}
