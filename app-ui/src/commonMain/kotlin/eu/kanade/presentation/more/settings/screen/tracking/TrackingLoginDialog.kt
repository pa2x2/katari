package eu.kanade.presentation.more.settings.screen.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import mihon.entry.interactions.tracking.EntryTrackingAccount
import mihon.entry.interactions.tracking.EntryTrackingAccountOperationResult
import mihon.entry.interactions.tracking.EntryTrackingCredentialIdentity
import mihon.entry.interactions.tracking.EntryTrackingFeature
import mihon.entry.interactions.tracking.EntryTrackingLoginMethod
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.field.SecureTextField
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Signs in to a tracker that takes a username or email and a password. */
@Composable
internal fun TrackingLoginDialog(
    account: EntryTrackingAccount,
    storedUsername: String,
    storedPassword: String,
    trackingFeature: EntryTrackingFeature,
    onDismissRequest: () -> Unit,
) {
    val messages = rememberScreenMessages()
    val scope = rememberCoroutineScope()

    val username = remember(storedUsername) { TextFieldState(storedUsername) }
    val password = remember(storedPassword) { TextFieldState(storedPassword) }
    var processing by remember { mutableStateOf(false) }
    var inputError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.login_title, account.service.name),
        confirm = DialogAction(
            label = stringResource(if (processing) MR.strings.logging_in else MR.strings.login),
            enabled = !processing && username.text.isNotBlank() && password.text.isNotBlank(),
            onClick = {
                scope.launchIO {
                    processing = true
                    val result = trackingFeature.loginWithCredentials(
                        serviceId = account.service.id,
                        username = username.text.toString(),
                        password = password.text.toString(),
                    )
                    inputError = result !is EntryTrackingAccountOperationResult.Completed
                    when (result) {
                        EntryTrackingAccountOperationResult.Completed -> withUIContext {
                            messages.show(MR.strings.login_success)
                            onDismissRequest()
                        }
                        is EntryTrackingAccountOperationResult.Failed -> withUIContext {
                            messages.show(result.cause.message.toString())
                        }
                        is EntryTrackingAccountOperationResult.ExternalAuthorization,
                        is EntryTrackingAccountOperationResult.Unavailable,
                        -> Unit
                    }
                    processing = false
                }
            },
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            val usernameLabel = when ((account.loginMethod as EntryTrackingLoginMethod.Credentials).identity) {
                EntryTrackingCredentialIdentity.USERNAME -> MR.strings.username
                EntryTrackingCredentialIdentity.EMAIL -> MR.strings.email
            }
            TextField(
                state = username,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Username + ContentType.EmailAddress },
                label = stringResource(usernameLabel),
                isError = inputError && !processing,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            var hidePassword by remember { mutableStateOf(true) }
            SecureTextField(
                state = password,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentType = ContentType.Password },
                label = stringResource(MR.strings.password),
                isError = inputError && !processing,
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
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            )
        }
    }
}
