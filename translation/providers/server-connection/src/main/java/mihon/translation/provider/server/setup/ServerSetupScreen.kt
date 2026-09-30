package mihon.translation.provider.server.setup

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import kotlinx.coroutines.launch
import mihon.translation.provider.server.R
import mihon.translation.provider.server.ServerEndpointPolicy
import mihon.translation.provider.server.setup.components.ProviderInformationCard
import mihon.translation.provider.server.setup.components.ProviderSetupHeader
import mihon.translation.provider.server.setup.components.ProviderSetupPrimaryButton
import mihon.translation.provider.server.setup.components.ProviderSetupScaffold
import mihon.translation.provider.server.setup.components.ProviderSetupStatus
import mihon.translation.provider.server.setup.components.ProviderSetupStatusPanel

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

/** Lets the user enter a server endpoint and API key, then saves and tests them through [coordinator]. */
@Composable
fun ServerSetupRoute(
    initialEndpoint: String,
    initialApiKey: String,
    coordinator: ServerSetupCoordinator,
    texts: ServerSetupTexts,
    @DrawableRes artworkResourceId: Int,
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
    val readyMessage = stringResource(R.string.server_connection_ready)
    val saveFailureMessage = stringResource(R.string.server_connection_save_failed)

    ServerSetupScreen(
        texts = texts,
        artworkResourceId = artworkResourceId,
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
    @DrawableRes artworkResourceId: Int,
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
    ProviderSetupScaffold(
        title = texts.title,
        backContentDescription = stringResource(R.string.translation_provider_back),
        onBack = onBack,
    ) {
        ProviderSetupHeader(
            artworkResourceId = artworkResourceId,
            title = texts.title,
            description = texts.description,
        )
        ProviderInformationCard(
            title = stringResource(R.string.server_connection_security_title),
            lines = listOf(stringResource(R.string.server_connection_security_description)),
        )
        OutlinedTextField(
            state = rememberSyncedTextFieldState(endpoint, onEndpointChange),
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = { Text(stringResource(R.string.server_connection_endpoint)) },
            supportingText = if (endpointInvalid) {
                {
                    Text(stringResource(R.string.server_connection_invalid_endpoint))
                }
            } else {
                null
            },
            isError = endpointInvalid,
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        if (endpointSuggestions.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                endpointSuggestions.forEach { suggestion ->
                    FilterChip(
                        selected = ServerEndpointPolicy.validate(endpoint)?.toString() == suggestion.endpoint,
                        onClick = { onEndpointChange(suggestion.endpoint) },
                        label = { Text(suggestion.label) },
                        enabled = !testing,
                    )
                }
            }
        }
        OutlinedSecureTextField(
            state = rememberSyncedTextFieldState(apiKey, onApiKeyChange),
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = { Text(texts.apiKeyLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textObfuscationMode = if (apiKeyVisible) {
                TextObfuscationMode.Visible
            } else {
                TextObfuscationMode.Hidden
            },
            trailingIcon = {
                IconButton(
                    onClick = { apiKeyVisible = !apiKeyVisible },
                    enabled = !testing,
                ) {
                    Icon(
                        imageVector = if (apiKeyVisible) {
                            Icons.Outlined.VisibilityOff
                        } else {
                            Icons.Outlined.Visibility
                        },
                        contentDescription = stringResource(
                            if (apiKeyVisible) {
                                R.string.translation_provider_hide_api_key
                            } else {
                                R.string.translation_provider_show_api_key
                            },
                        ),
                    )
                }
            },
        )
        status?.let {
            ProviderSetupStatusPanel(
                status = it,
                testingLabel = stringResource(R.string.translation_provider_status_testing),
                successLabel = stringResource(R.string.translation_provider_status_success),
                failureLabel = stringResource(R.string.translation_provider_status_failure),
            )
        }
        if (status is ProviderSetupStatus.Success) {
            ProviderSetupPrimaryButton(
                label = stringResource(R.string.translation_provider_done),
                enabled = true,
                onClick = onDone,
            )
        } else {
            ProviderSetupPrimaryButton(
                label = stringResource(R.string.server_connection_save_and_test),
                enabled = !testing,
                onClick = onTest,
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
    TachiyomiPreviewTheme {
        ServerSetupScreen(
            texts = ServerSetupTexts(
                title = "Translation server setup",
                description = "Translation through a server you choose.",
                apiKeyLabel = "API key (optional)",
                connectionFailed = "Katari could not validate this server.",
            ),
            artworkResourceId = android.R.drawable.ic_menu_manage,
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
