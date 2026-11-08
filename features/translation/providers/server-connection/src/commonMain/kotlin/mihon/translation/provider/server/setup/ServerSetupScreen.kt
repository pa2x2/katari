package mihon.translation.provider.server.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Translate
import mihon.icons.materialsymbols.rounded.Visibility
import mihon.icons.materialsymbols.rounded.VisibilityOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.chip.FilterChip
import mihon.presentation.kit.field.SecureTextField
import mihon.presentation.kit.field.TextField
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.translation.provider.server.ServerEndpointPolicy
import mihon.translation.provider.server.setup.components.ProviderInformationCard
import mihon.translation.provider.server.setup.components.ProviderSetupHeader
import mihon.translation.provider.server.setup.components.ProviderSetupScaffold
import mihon.translation.provider.server.setup.components.ProviderSetupStatus
import mihon.translation.provider.server.setup.components.ProviderSetupStatusPanel
import tachiyomi.i18n.MR
import tachiyomi.i18n.server_connection_endpoint
import tachiyomi.i18n.server_connection_invalid_endpoint
import tachiyomi.i18n.server_connection_ready
import tachiyomi.i18n.server_connection_save_and_test
import tachiyomi.i18n.server_connection_save_failed
import tachiyomi.i18n.server_connection_security_title
import tachiyomi.i18n.translation_provider_done
import tachiyomi.i18n.translation_provider_hide_api_key
import tachiyomi.i18n.translation_provider_show_api_key
import tachiyomi.i18n.translation_provider_status_failure
import tachiyomi.i18n.translation_provider_status_success
import tachiyomi.i18n.translation_provider_status_testing
import tachiyomi.presentation.core.i18n.stringResource

/** What a provider's server setup screen says about that provider. */
data class ServerSetupTexts(
    val title: String,
    val description: String,
    val apiKeyLabel: String,
    val connectionFailed: String,
)

/** A well-known endpoint the user can fill in with one tap. */
data class ServerEndpointSuggestion(
    val label: String,
    val endpoint: String,
)

/**
 * Lets the user enter a server endpoint and API key, then saves and tests them through [coordinator].
 * [securityDescription] tells where the key is kept.
 */
@Composable
fun ServerSetupRoute(
    initialEndpoint: String,
    initialApiKey: String,
    coordinator: ServerSetupCoordinator,
    texts: ServerSetupTexts,
    securityDescription: String,
    artwork: Painter,
    onBack: () -> Unit,
    onDone: () -> Unit,
    endpointSuggestions: List<ServerEndpointSuggestion> = emptyList(),
) {
    var endpoint by rememberSaveable { mutableStateOf(initialEndpoint) }
    var apiKey by rememberSaveable { mutableStateOf(initialApiKey) }
    var endpointInvalid by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<ProviderSetupStatus?>(null) }
    val scope = rememberCoroutineScope()
    val testing = status == ProviderSetupStatus.Testing
    val readyMessage = stringResource(MR.strings.server_connection_ready)
    val saveFailureMessage = stringResource(MR.strings.server_connection_save_failed)

    ServerSetupScreen(
        texts = texts,
        securityDescription = securityDescription,
        artwork = artwork,
        endpointSuggestions = endpointSuggestions,
        endpoint = endpoint,
        apiKey = apiKey,
        endpointInvalid = endpointInvalid,
        status = status,
        testing = testing,
        onEndpointChange = {
            endpoint = it
            endpointInvalid = false
            status = null
        },
        onApiKeyChange = {
            apiKey = it
            status = null
        },
        onTest = {
            if (ServerEndpointPolicy.validate(endpoint) == null) {
                endpointInvalid = true
                return@ServerSetupScreen
            }
            status = ProviderSetupStatus.Testing
            scope.launch {
                status = when (val result = coordinator.saveAndTest(endpoint, apiKey)) {
                    ServerSetupResult.InvalidEndpoint -> {
                        endpointInvalid = true
                        null
                    }
                    ServerSetupResult.Ready -> ProviderSetupStatus.Success(readyMessage)
                    ServerSetupResult.ConnectionFailed -> ProviderSetupStatus.Failure(texts.connectionFailed)
                    is ServerSetupResult.LocalNetworkAccessDenied -> ProviderSetupStatus.Failure(result.message)
                    ServerSetupResult.SaveFailed -> ProviderSetupStatus.Failure(saveFailureMessage)
                }
            }
        },
        onBack = onBack,
        onDone = onDone,
    )
}

@Composable
private fun ServerSetupScreen(
    texts: ServerSetupTexts,
    securityDescription: String,
    artwork: Painter,
    endpointSuggestions: List<ServerEndpointSuggestion>,
    endpoint: String,
    apiKey: String,
    endpointInvalid: Boolean,
    status: ProviderSetupStatus?,
    testing: Boolean,
    onEndpointChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onTest: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
    ProviderSetupScaffold(title = texts.title, onBack = onBack) {
        ProviderSetupHeader(
            artwork = artwork,
            title = texts.title,
            description = texts.description,
        )
        ProviderInformationCard(
            title = stringResource(MR.strings.server_connection_security_title),
            lines = listOf(securityDescription),
        )
        TextField(
            state = rememberSyncedTextFieldState(endpoint, onEndpointChange),
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = stringResource(MR.strings.server_connection_endpoint),
            supportingText = stringResource(MR.strings.server_connection_invalid_endpoint).takeIf { endpointInvalid },
            isError = endpointInvalid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        if (endpointSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                endpointSuggestions.forEach { suggestion ->
                    FilterChip(
                        label = suggestion.label,
                        selected = ServerEndpointPolicy.validate(endpoint)?.toString() == suggestion.endpoint,
                        onClick = { onEndpointChange(suggestion.endpoint) },
                        enabled = !testing,
                    )
                }
            }
        }
        SecureTextField(
            state = rememberSyncedTextFieldState(apiKey, onApiKeyChange),
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = texts.apiKeyLabel,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            revealed = apiKeyVisible,
            trailingIcon = {
                IconButton(
                    icon = if (apiKeyVisible) {
                        MaterialSymbols.Rounded.VisibilityOff
                    } else {
                        MaterialSymbols.Rounded.Visibility
                    },
                    contentDescription = stringResource(
                        if (apiKeyVisible) {
                            MR.strings.translation_provider_hide_api_key
                        } else {
                            MR.strings.translation_provider_show_api_key
                        },
                    ),
                    onClick = { apiKeyVisible = !apiKeyVisible },
                    enabled = !testing,
                )
            },
        )
        status?.let {
            ProviderSetupStatusPanel(
                status = it,
                testingLabel = stringResource(MR.strings.translation_provider_status_testing),
                successLabel = stringResource(MR.strings.translation_provider_status_success),
                failureLabel = stringResource(MR.strings.translation_provider_status_failure),
            )
        }
        if (status is ProviderSetupStatus.Success) {
            Button(
                label = stringResource(MR.strings.translation_provider_done),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Button(
                label = stringResource(MR.strings.server_connection_save_and_test),
                onClick = onTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !testing,
            )
        }
    }
}

/** Bridges a hoisted [String] value with a state-based text field, preserving external updates. */
@Composable
private fun rememberSyncedTextFieldState(
    text: String,
    onTextChange: (String) -> Unit,
): TextFieldState {
    val state = rememberTextFieldState(text)
    val currentText by rememberUpdatedState(text)
    val currentOnTextChange by rememberUpdatedState(onTextChange)
    LaunchedEffect(text) {
        if (state.text.toString() != text) {
            state.edit { replace(0, length, text) }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect {
            if (it != currentText) currentOnTextChange(it)
        }
    }
    return state
}

@Preview(name = "Server setup states", showBackground = true)
@Composable
private fun ServerSetupPreview(
    @PreviewParameter(ServerSetupPreviewStateProvider::class) status: ProviderSetupStatus?,
) {
    KatariPreviewTheme {
        ServerSetupScreen(
            texts = ServerSetupTexts(
                title = "Translation server setup",
                description = "Translation through a server you choose.",
                apiKeyLabel = "API key (optional)",
                connectionFailed = "Katari could not validate this server.",
            ),
            securityDescription = "HTTPS is required for remote servers. Your API key is kept safe.",
            artwork = rememberVectorPainter(MaterialSymbols.Rounded.Translate),
            endpointSuggestions = listOf(ServerEndpointSuggestion("Example", "https://translate.example/")),
            endpoint = "https://translate.example/",
            apiKey = "private-key",
            endpointInvalid = false,
            status = status,
            testing = status == ProviderSetupStatus.Testing,
            onEndpointChange = {},
            onApiKeyChange = {},
            onTest = {},
            onBack = {},
            onDone = {},
        )
    }
}

private class ServerSetupPreviewStateProvider : PreviewParameterProvider<ProviderSetupStatus?> {
    override val values = sequenceOf(
        null,
        ProviderSetupStatus.Testing,
        ProviderSetupStatus.Success("Ready to use"),
        ProviderSetupStatus.Failure("Check the server address and try again."),
    )
}
