package app.katari.android.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.sync.ui.instant.InstantSyncSetup
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.general.UnrestrictedBackground
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Says what instant sync costs and, once the person agrees, asks to be exempted from battery optimisation, which would
 * otherwise cut the connection while the phone sleeps. Then it offers to hide the notification Android makes the app
 * run under, by opening that notification's settings ([InstantSyncNotification]), unless the person has hidden it
 * already.
 */
@Inject
class AndroidInstantSyncSetup : InstantSyncSetup {
    override val isOffered: Boolean = true

    @Composable
    override fun Setup(onTurnOn: () -> Unit, onClose: () -> Unit) {
        val context = currentPlatformContext()
        var turnedOn by rememberSaveable { mutableStateOf(false) }
        if (!turnedOn) {
            ConfirmDialog(
                title = stringResource(PlatformStrings.sync_instant_title),
                text = stringResource(PlatformStrings.sync_instant_setup),
                confirmLabel = stringResource(PlatformStrings.sync_instant_turn_on),
                onConfirm = {
                    onTurnOn()
                    UnrestrictedBackground.request(context)
                    if (InstantSyncNotification.isShown(context)) turnedOn = true else onClose()
                },
                onDismissRequest = onClose,
            )
        } else {
            ConfirmDialog(
                title = stringResource(PlatformStrings.sync_instant_hide_title),
                text = stringResource(PlatformStrings.sync_instant_hide_setup),
                confirmLabel = stringResource(PlatformStrings.sync_instant_hide),
                dismissLabel = stringResource(PlatformStrings.sync_instant_keep_notification),
                onConfirm = {
                    context.startActivity(InstantSyncNotification.channelSettingsIntent(context))
                    onClose()
                },
                onDismissRequest = onClose,
            )
        }
    }
}
