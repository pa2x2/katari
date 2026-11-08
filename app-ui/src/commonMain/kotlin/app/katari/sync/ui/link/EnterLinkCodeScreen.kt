package app.katari.sync.ui.link

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
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.sync.client.account.LinkCodeLink
import app.katari.sync.client.account.ServerCheck
import app.katari.sync.ui.address.AddressCheck
import app.katari.sync.ui.signin.JoinPath
import app.katari.sync.ui.signin.JoinProblemNote
import app.katari.sync.ui.signin.OneTimeCodeField
import app.katari.sync.ui.signin.ServerCheckProblemNote
import app.katari.sync.ui.signin.SyncJoinViewModel
import app.katari.sync.ui.signin.joinProblemText
import app.katari.sync.ui.signin.leaveJoined
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.drop
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Typing a linked device's code (D027, D051): on desktop in place of scanning, and on a phone whose camera can't read
 * it. A typed code doesn't carry the server, so its address is typed beside it and checked like at sign-in, and
 * doesn't carry the account, so Join looks the code up and asks about joining that account.
 */
class EnterLinkCodeScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<SyncJoinViewModel>()
        val check by model.check.collectAsState()
        val join by model.join.collectAsState()
        val lookUp by model.lookUp.collectAsState()
        val address = rememberTextFieldState()
        val code = rememberTextFieldState()
        LaunchedEffect(address) { snapshotFlow { address.text.toString() }.collect(model::addressChanged) }
        LaunchedEffect(address, code) {
            snapshotFlow { address.text.toString() to code.text.toString() }
                .drop(1)
                .collect { model.forgetFailedLookUp() }
        }
        LaunchedEffect(join) {
            (join as? SyncJoinViewModel.JoinState.Joined)?.let(navigator::leaveJoined)
        }
        val server = ((check as? AddressCheck.Checked)?.check as? ServerCheck.Ready)?.server

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.sync_enter_code_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    val lookingUp = lookUp == SyncJoinViewModel.LookUpState.LookingUp
                    Button(
                        label = stringResource(
                            if (lookingUp) MR.strings.sync_link_code_checking else MR.strings.sync_action_join,
                        ),
                        onClick = {
                            model.clearResult()
                            model.lookUpLinkCode(address.text.toString(), code.text.toString())
                        },
                        modifier = Modifier.weight(1f),
                        enabled = server != null && code.text.isNotBlank() && !lookingUp,
                        loading = lookingUp,
                    )
                }
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                item { RowGroupNote(stringResource(MR.strings.sync_enter_code_hint)) }
                item {
                    RowGroupItem(RowGroupPosition.Alone) {
                        ControlRow {
                            Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
                                TextField(
                                    state = address,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = stringResource(MR.strings.sync_server_address),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Next,
                                    ),
                                )
                                OneTimeCodeField(
                                    state = code,
                                    label = stringResource(MR.strings.sync_link_code),
                                    imeAction = ImeAction.Done,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                (lookUp as? SyncJoinViewModel.LookUpState.Failed)?.let { failed ->
                    item { JoinProblemNote(failed.result, JoinPath.LINK_CODE) }
                }
                item {
                    when (val shown = check) {
                        is AddressCheck.Checked -> {
                            val result = shown.check
                            if (result is ServerCheck.Ready && result.isPlainHttp) {
                                RowGroupNote(
                                    stringResource(MR.strings.sync_server_plain_http_local),
                                    tone = ContentTone.Warning,
                                )
                            } else {
                                ServerCheckProblemNote(result)
                            }
                        }
                        AddressCheck.Checking ->
                            RowGroupNote(stringResource(MR.strings.sync_server_checking))
                        AddressCheck.Empty -> Unit
                    }
                }
            }
        }

        val found = lookUp as? SyncJoinViewModel.LookUpState.Found
        if (found != null && server != null) {
            val failed = join as? SyncJoinViewModel.JoinState.Failed
            JoinConfirmDialog(
                link = LinkCodeLink(server, code.text.toString().trim(), found.accountName, found.fromDevice),
                joining = join == SyncJoinViewModel.JoinState.Joining,
                problem = failed?.let { joinProblemText(it.result, it.path) },
                onJoin = { model.redeemLinkCode(address.text.toString(), code.text.toString()) },
                onDismissRequest = model::forgetLookUp,
            )
        }
    }
}
