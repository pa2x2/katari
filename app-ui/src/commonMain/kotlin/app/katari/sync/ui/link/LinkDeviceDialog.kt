package app.katari.sync.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.delay
import mihon.presentation.kit.content.QrCode
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.layout.FigureWithText
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Link a device (D027): a QR code for a phone to scan and the same code with the server's address for typing on a
 * desktop, valid once for two minutes, with the time left. An expired code is replaced on request; once a device used
 * the code, the dialog names it instead.
 */
@Composable
internal fun LinkDeviceDialog(onDismissRequest: () -> Unit) {
    val model = metroViewModel<LinkDeviceViewModel>()
    val state by model.code.collectAsState()
    LaunchedEffect(Unit) { model.newCode() }
    val now by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            delay(1.seconds)
            value = Clock.System.now().toEpochMilliseconds()
        }
    }
    val shown = state as? LinkDeviceViewModel.LinkCodeState.Shown
    val expired = shown != null && now >= shown.expiresAtMillis

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.sync_link_device),
        confirm = DialogAction(stringResource(MR.strings.action_done), onDismissRequest),
        neutral = DialogAction(stringResource(MR.strings.sync_action_new_code), model::newCode)
            .takeIf { expired || state == LinkDeviceViewModel.LinkCodeState.Unavailable },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        ) {
            when (val code = state) {
                LinkDeviceViewModel.LinkCodeState.Loading -> LoadingIndicator()
                LinkDeviceViewModel.LinkCodeState.Unavailable ->
                    Text(stringResource(MR.strings.sync_link_unavailable), tone = ContentTone.Error)
                is LinkDeviceViewModel.LinkCodeState.Used ->
                    Text(stringResource(MR.strings.sync_link_used, code.deviceName), textAlign = TextAlign.Center)
                is LinkDeviceViewModel.LinkCodeState.Shown -> if (expired) {
                    Text(stringResource(MR.strings.sync_link_expired), textAlign = TextAlign.Center)
                } else {
                    FigureWithText(
                        figure = {
                            QrCode(
                                text = code.offer.link.encode(),
                                contentDescription = stringResource(MR.strings.sync_link_qr_description),
                            )
                        },
                    ) {
                        Text(code.offer.code, role = TextRole.TypedCode, textAlign = TextAlign.Center)
                        Text(code.offer.server.toString(), role = TextRole.Body, textAlign = TextAlign.Center)
                        Text(
                            stringResource(MR.strings.sync_link_instructions),
                            role = TextRole.Supporting,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            stringResource(MR.strings.sync_link_expires, timeLeft(code.expiresAtMillis - now)),
                            role = TextRole.Supporting,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** Minutes and seconds, such as 1:41. */
private fun timeLeft(millis: Long): String {
    val left = millis.coerceAtLeast(0).milliseconds
    return "${left.inWholeMinutes}:${(left.inWholeSeconds % 60).toString().padStart(2, '0')}"
}
