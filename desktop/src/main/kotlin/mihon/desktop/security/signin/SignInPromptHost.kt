package mihon.desktop.security.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.SecureTextField
import mihon.presentation.kit.list.InfoRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Shows the sign-in check [prompts] waits on, if any, as a dialog in the window. */
@Composable
internal fun SignInPromptHost(prompts: SignInPrompts, signIn: SystemSignIn) {
    val request by prompts.current.collectAsState()
    request?.let { key(it) { SignInDialog(it, signIn) } }
}

/**
 * Checks the sign-in for [request]: it shows what the system says and asks, sends the password once the system asks
 * for it, and tries again after a wrong one. Closing the dialog stops the check, which the system then doesn't count.
 */
@Composable
private fun SignInDialog(request: SignInRequest, signIn: SystemSignIn) {
    val state = remember { SignInCheckState() }
    val password = rememberTextFieldState()
    val focus = remember { FocusRequester() }
    var revealed by remember { mutableStateOf(false) }
    var tries by remember { mutableIntStateOf(0) }
    var run by remember { mutableStateOf<SignInCheckRun?>(null) }

    LaunchedEffect(tries) {
        state.startTry()
        val started = withContext(Dispatchers.IO) { signIn.start() } ?: return@LaunchedEffect state.couldNotStart()
        run = started
        started.events.collect { event ->
            when (state.take(event)) {
                SignInOutcome.Accepted -> request.answer.complete(true)
                SignInOutcome.Refused -> {
                    password.clearText()
                    tries++
                }
                SignInOutcome.Failed, null -> Unit
            }
        }
    }
    val question = state.question
    LaunchedEffect(question) {
        if (question != null) focus.requestFocus()
    }

    val cancel = { request.answer.complete(false) }
    val confirm = when {
        state.failed -> DialogAction(stringResource(MR.strings.action_retry), onClick = { tries++ })
        state.checking -> DialogAction(stringResource(MR.strings.sign_in_check_checking), {
        }, enabled = false, loading = true)
        else -> DialogAction(
            label = request.confirmLabel,
            onClick = {
                run?.answer(password.text.toString())
                state.answered()
            },
            enabled = question != null && password.text.isNotEmpty(),
        )
    }
    Dialog(
        onDismissRequest = { cancel() },
        title = request.title,
        confirm = confirm,
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = { cancel() }),
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            request.detail?.let { Text(it) }
            Text(stringResource(MR.strings.sign_in_check_line, signIn.account))
            state.messages.forEach { InfoRow(it.text) }
            SecureTextField(
                state = password,
                modifier = Modifier.focusRequester(focus),
                label = question?.prompt?.trim()?.removeSuffix(":") ?: stringResource(MR.strings.password),
                supportingText = when {
                    state.failed -> stringResource(MR.strings.sign_in_check_failed)
                    state.refused -> stringResource(MR.strings.sign_in_check_refused)
                    state.waiting -> stringResource(MR.strings.sign_in_check_waiting)
                    else -> null
                },
                isError = state.failed || state.refused,
                enabled = question != null,
                revealed = revealed || question?.hidden == false,
                trailingIcon = {
                    IconButton(
                        icon = if (revealed) {
                            MaterialSymbols.Rounded.VisibilityOff
                        } else {
                            MaterialSymbols.Rounded.Visibility
                        },
                        contentDescription = stringResource(
                            if (revealed) MR.strings.action_hide_password else MR.strings.action_show_password,
                        ),
                        onClick = { revealed = !revealed },
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
        }
    }
}
