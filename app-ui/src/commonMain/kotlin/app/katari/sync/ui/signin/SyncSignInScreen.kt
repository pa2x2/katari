package app.katari.sync.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.ui.page.SyncScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.field.SecureTextField
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.list.RowGroupGap
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupNote
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Signing in with a server address (D027): the address is checked as it is typed, then a username and password, or
 * an invite code with the username and password of the new account. [server] fills the address in, such as when
 * signing in again to the server the device was removed from.
 */
class SyncSignInScreen(private val server: String? = null) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<SyncJoinViewModel>()
        val check by model.check.collectAsState()
        val join by model.join.collectAsState()

        val address = rememberTextFieldState(server.orEmpty())
        val username = rememberTextFieldState()
        val password = rememberTextFieldState()
        val invite = rememberTextFieldState()
        var withInvite by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(address) {
            snapshotFlow { address.text.toString() }.collect {
                model.addressChanged(it)
                model.clearResult()
            }
        }
        LaunchedEffect(join) {
            if (join == SyncJoinViewModel.JoinState.Joined) navigator.popUntil { it == SyncScreen }
        }

        val joining = join == SyncJoinViewModel.JoinState.Joining
        val serverReady = (check as? SyncJoinViewModel.AddressCheck.Checked)?.check is ServerCheck.Ready
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
                                modifier = Modifier.fillMaxWidth(),
                                label = stringResource(MR.strings.sync_server_address),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Uri,
                                    imeAction = ImeAction.Next,
                                ),
                            )
                        }
                    }
                }
                item { AddressNote(check) }
                item { RowGroupGap() }
                item {
                    RowGroupItem(RowGroupPosition.Alone) {
                        ControlRow {
                            CredentialFields(withInvite, invite, username, password)
                        }
                    }
                }
                (join as? SyncJoinViewModel.JoinState.Failed)?.let { failed ->
                    item {
                        joinProblemText(failed.result, failed.path)?.let {
                            RowGroupNote(it, tone = ContentTone.Error)
                        }
                    }
                }
                item {
                    Box(Modifier.padding(horizontal = KatariTheme.spacing.m)) {
                        Button(
                            label = if (withInvite) {
                                stringResource(MR.strings.sync_action_use_account)
                            } else {
                                stringResource(MR.strings.sync_action_use_invite)
                            },
                            onClick = {
                                withInvite = !withInvite
                                model.clearResult()
                            },
                            style = ButtonStyle.Text,
                        )
                    }
                }
            }
        }
    }
}

/** What checking the address found, under the address: a warning, a refusal, or that it is being checked. */
@Composable
private fun AddressNote(check: SyncJoinViewModel.AddressCheck) {
    when (check) {
        SyncJoinViewModel.AddressCheck.Empty -> Unit
        SyncJoinViewModel.AddressCheck.Checking -> RowGroupNote(stringResource(MR.strings.sync_server_checking))
        is SyncJoinViewModel.AddressCheck.Checked -> when (val result = check.check) {
            is ServerCheck.Ready -> if (result.isPlainHttp) {
                RowGroupNote(stringResource(MR.strings.sync_server_plain_http_local), tone = ContentTone.Warning)
            }
            is ServerCheck.NeedsSetup -> {
                val uriHandler = LocalUriHandler.current
                if (!result.webPage.isHttps) {
                    RowGroupNote(stringResource(MR.strings.sync_server_plain_http_local), tone = ContentTone.Warning)
                }
                serverCheckProblemText(result)?.let { RowGroupNote(it, showIcon = true) }
                Box(Modifier.padding(horizontal = KatariTheme.spacing.m)) {
                    Button(
                        label = stringResource(MR.strings.action_open_in_browser),
                        onClick = { uriHandler.openUri(result.webPage.toString()) },
                        style = ButtonStyle.Text,
                    )
                }
            }
            else -> serverCheckProblemText(result)?.let { RowGroupNote(it, tone = ContentTone.Error) }
        }
    }
}

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
