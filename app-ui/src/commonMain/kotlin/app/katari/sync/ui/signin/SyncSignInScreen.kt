package app.katari.sync.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Visibility
import app.katari.icons.materialsymbols.rounded.VisibilityOff
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.field.SecureTextField
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroupAction
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.client.connection.SyncStop
import app.katari.sync.client.server.shownAddress
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.address.AddressCheck
import app.katari.sync.ui.guide.ServerFixAction
import app.katari.sync.ui.status.rememberSyncStatus
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import okhttp3.HttpUrl
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Signing in with a server address (D027): the address is checked as it is typed, then a username and password, or
 * an invite code with the username and password of the new account. [server] and [account] fill the address and
 * username in, such as when signing in again to the server the device was removed from; [inviteCode] opens it on
 * signing up with that invite, as an invite's QR code does.
 */
class SyncSignInScreen(
    private val server: String? = null,
    private val account: String? = null,
    private val inviteCode: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<SyncJoinViewModel>()
        val check by model.check.collectAsState()
        val join by model.join.collectAsState()

        val address = rememberTextFieldState(server.orEmpty())
        val username = rememberTextFieldState(account.orEmpty())
        val password = rememberTextFieldState()
        val invite = rememberTextFieldState(inviteCode.orEmpty())
        var withInvite by rememberSaveable { mutableStateOf(inviteCode != null) }

        LaunchedEffect(address) {
            snapshotFlow { address.text.toString() }.collect {
                model.addressChanged(it)
                model.clearResult()
            }
        }
        LaunchedEffect(join) {
            (join as? SyncJoinViewModel.JoinState.Joined)?.let(navigator::leaveJoined)
        }
        // Back from the server's web page, where an empty server's admin account is made, the server can sign in now
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { model.recheckAddress() }
        val addressFocus = remember { FocusRequester() }
        // The keyboard opens in the address field once, not each time the field scrolls back into view, and not when
        // the address came filled in, such as from an invite's QR code
        var addressFocused by rememberSaveable { mutableStateOf(server != null) }

        val joining = join == SyncJoinViewModel.JoinState.Joining
        val serverReady = (check as? AddressCheck.Checked)?.check is ServerCheck.Ready
        val canSubmit = !joining && serverReady && username.text.isNotBlank() && password.text.isNotEmpty() &&
            (!withInvite || invite.text.isNotBlank())
        val submit = {
            if (withInvite) {
                model.redeemInvite(
                    address.text.toString(),
                    invite.text.toString(),
                    username.text.toString(),
                    password.text.toString(),
                )
            } else {
                model.signIn(address.text.toString(), username.text.toString(), password.text.toString())
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_sign_in_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    Button(
                        label = stringResource(
                            if (withInvite) MR.strings.sync_action_create_account else MR.strings.sync_action_sign_in,
                        ),
                        onClick = submit,
                        modifier = Modifier.weight(1f),
                        enabled = canSubmit,
                        loading = joining,
                    )
                }
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                item {
                    RowGroupItem(RowGroupPosition.Alone) {
                        ControlRow {
                            TextField(
                                state = address,
                                modifier = Modifier.fillMaxWidth().focusRequester(addressFocus),
                                label = stringResource(MR.strings.sync_server_address),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                            LaunchedEffect(Unit) {
                                if (!addressFocused) {
                                    addressFocus.requestFocus()
                                    addressFocused = true
                                }
                            }
                        }
                    }
                }
                item { AddressNote(check, resetServer = rememberResetServer()) }
                item { RowGroupGap() }
                item {
                    RowGroupItem(RowGroupPosition.Alone) {
                        ControlRow {
                            CredentialFields(withInvite, invite, username, password)
                        }
                    }
                }
                (join as? SyncJoinViewModel.JoinState.Failed)?.let { failed ->
                    item { JoinProblemNote(failed.result, failed.path) }
                }
                item {
                    RowGroupAction(
                        label = if (withInvite) {
                            stringResource(MR.strings.sync_action_use_account)
                        } else {
                            stringResource(MR.strings.sync_action_use_invite)
                        },
                        onClick = {
                            withInvite = !withInvite
                            model.clearResult()
                        },
                    )
                }
            }
        }
    }
}

/**
 * What checking the address found, under the address: a warning, a refusal, or that it is being checked. An empty
 * server at [resetServer], the one this device synced with before it was reset, says so.
 */
@Composable
private fun AddressNote(check: AddressCheck, resetServer: HttpUrl?) {
    when (check) {
        AddressCheck.Empty -> Unit
        AddressCheck.Checking -> RowGroupNote(stringResource(MR.strings.sync_server_checking))
        is AddressCheck.Checked -> when (val result = check.check) {
            is ServerCheck.Ready -> if (result.isPlainHttp) {
                RowGroupNote(stringResource(MR.strings.sync_server_plain_http_local), tone = ContentTone.Warning)
            }
            is ServerCheck.NeedsSetup -> {
                val uriHandler = LocalUriHandler.current
                if (!result.webPage.isHttps) {
                    RowGroupNote(stringResource(MR.strings.sync_server_plain_http_local), tone = ContentTone.Warning)
                }
                val setUp = if (result.webPage == resetServer) {
                    stringResource(MR.strings.sync_server_reset_needs_setup, result.webPage.shownAddress)
                } else {
                    serverCheckProblemText(result)
                }
                setUp?.let { RowGroupNote(it, showIcon = true) }
                RowGroupAction(
                    label = stringResource(MR.strings.action_open_in_browser),
                    onClick = { uriHandler.openUri(result.webPage.toString()) },
                )
                serverCheckFix(result)?.let { ServerFixAction(it) }
            }
            else -> ServerCheckProblemNote(result)
        }
    }
}

/** The server this device synced with, while it waits to sign in again because that server was reset. */
@Composable
private fun rememberResetServer(): HttpUrl? =
    (rememberSyncStatus()?.link as? SyncStatus.Link.Linked)?.takeIf { it.stopped == SyncStop.SERVER_RESET }?.server

@Composable
private fun CredentialFields(
    withInvite: Boolean,
    invite: TextFieldState,
    username: TextFieldState,
    password: TextFieldState,
) {
    var hidePassword by rememberSaveable { mutableStateOf(true) }
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
        if (withInvite) {
            TextField(
                state = invite,
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(MR.strings.sync_invite_code),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )
        }
        TextField(
            state = username,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = if (withInvite) ContentType.NewUsername else ContentType.Username },
            label = stringResource(if (withInvite) MR.strings.sync_choose_username else MR.strings.username),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        SecureTextField(
            state = password,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = if (withInvite) ContentType.NewPassword else ContentType.Password },
            label = stringResource(if (withInvite) MR.strings.sync_choose_password else MR.strings.password),
            revealed = !hidePassword,
            trailingIcon = {
                IconButton(
                    icon = if (hidePassword) {
                        MaterialSymbols.Rounded.Visibility
                    } else {
                        MaterialSymbols.Rounded.VisibilityOff
                    },
                    contentDescription = stringResource(
                        if (hidePassword) MR.strings.action_show_password else MR.strings.action_hide_password,
                    ),
                    onClick = { hidePassword = !hidePassword },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
    }
}
