package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState()
    val storeAlreadyExists by remember(storeIndexUrls) {
        derivedStateOf {
            val indexUrl = state.text.toString()
            storeIndexUrls.contains(indexUrl)
        }
    }

    val focusRequester = remember { FocusRequester() }

    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title),
        confirm = DialogAction(
            label = stringResource(addLabel(processing)),
            onClick = { onCreate(state.text.toString()) },
            enabled = !processing && state.text.isNotEmpty() && !storeAlreadyExists,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        TextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            label = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel),
            supportingText = errorMessage
                ?: stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists).takeIf {
                    storeAlreadyExists
                },
            isError = errorMessage != null || storeAlreadyExists,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
    }

    LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    ConfirmDialog(
        title = stringResource(MR.strings.extensionStoresScreen_deleteStore_title),
        text = stringResource(MR.strings.extensionStoresScreen_deleteStore_body, storeName, storeIndexUrl),
        confirmLabel = stringResource(MR.strings.action_delete),
        onConfirm = {
            onDelete()
            onDismissRequest()
        },
        onDismissRequest = onDismissRequest,
        destructive = true,
        icon = MaterialSymbols.Rounded.Delete,
    )
}

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.extensionStoresScreen_addStore_title),
        confirm = DialogAction(
            label = stringResource(addLabel(processing)),
            onClick = onCreate,
            enabled = !storeAlreadyExists && !processing,
        ),
        dismiss = DialogAction(stringResource(MR.strings.action_cancel), onClick = onDismissRequest),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
            Text(stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText))
            TextField(
                value = storeIndexUrl,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                supportingText = when {
                    storeAlreadyExists -> stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists)
                    else -> errorMessage
                },
                isError = errorMessage != null || storeAlreadyExists,
                readOnly = true,
            )
        }
    }
}

private fun addLabel(processing: Boolean) = if (processing) {
    MR.strings.extensionStoresScreen_addStore_processing
} else {
    MR.strings.action_add
}
