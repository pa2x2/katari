package app.katari.sync.ui.address

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.client.account.ServerMove
import app.katari.sync.client.server.shownAddress
import app.katari.sync.ui.signin.serverCheckProblemText
import dev.zacsweers.metrox.viewmodel.metroViewModel
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import okhttp3.HttpUrl
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Changes the address of this device's server, such as after it moved to a new IP address or domain. The new
 * address is checked as at sign-in, and the device keeps its sign-in only when the same server answers there; another
 * server is refused, and joining it is signing in anew.
 */
@Composable
internal fun ServerAddressDialog(current: HttpUrl, onDismissRequest: () -> Unit) {
    val model = metroViewModel<ServerAddressViewModel>()
    val check by model.check.collectAsState()
    val isThisServer by model.isThisServer.collectAsState()
    val move by model.move.collectAsState()
    val address = rememberTextFieldState(current.shownAddress)
    LaunchedEffect(address) {
        // The address the device syncs with now is no move, and checking it says nothing new
        snapshotFlow { address.text.toString() }
            .collect { model.addressChanged(if (it.trim() == current.shownAddress) "" else it) }
    }
    LaunchedEffect(move) { if (move == ServerAddressViewModel.MoveState.Moved) onDismissRequest() }

    val ready = (check as? AddressCheck.Checked)?.check is ServerCheck.Ready && isThisServer == true
    val moving = move == ServerAddressViewModel.MoveState.Moving
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.sync_server_address),
        confirm = DialogAction(
            label = stringResource(MR.strings.action_save),
            onClick = { model.moveTo(address.text.toString()) },
            enabled = ready && !moving,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            Text(stringResource(MR.strings.sync_server_address_text))
            TextField(
                state = address,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(MR.strings.sync_server_address),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
            )
            when (val failed = (move as? ServerAddressViewModel.MoveState.Failed)?.result) {
                null -> AddressNote(check, isThisServer)
                else -> MoveProblem(failed)
            }
        }
    }
}

/** What checking the new address found: a warning, why the device can't move there, or that it is being checked. */
@Composable
private fun AddressNote(check: AddressCheck, isThisServer: Boolean?) {
    when (check) {
        AddressCheck.Empty -> Unit
        AddressCheck.Checking -> Text(stringResource(MR.strings.sync_server_checking), role = TextRole.Supporting)
        is AddressCheck.Checked -> when (val result = check.check) {
            is ServerCheck.Ready -> when {
                isThisServer == false -> OtherServer()
                result.isPlainHttp -> Text(
                    stringResource(MR.strings.sync_server_plain_http_local),
                    role = TextRole.Supporting,
                    tone = ContentTone.Warning,
                )
            }
            // A server without accounts started over, or is another one
            is ServerCheck.NeedsSetup -> OtherServer()
            else -> serverCheckProblemText(result)?.let { Problem(it) }
        }
    }
}

@Composable
private fun MoveProblem(result: ServerMove) {
    when (result) {
        ServerMove.Moved -> Unit
        ServerMove.OtherServer -> OtherServer()
        is ServerMove.NotReady -> when (result.check) {
            is ServerCheck.NeedsSetup -> OtherServer()
            else -> serverCheckProblemText(result.check)?.let { Problem(it) }
        }
    }
}

@Composable
private fun OtherServer() = Problem(stringResource(MR.strings.sync_server_other))

@Composable
private fun Problem(text: String) = Text(text, role = TextRole.Supporting, tone = ContentTone.Error)
