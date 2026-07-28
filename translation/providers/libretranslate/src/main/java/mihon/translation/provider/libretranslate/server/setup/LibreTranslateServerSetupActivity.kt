package mihon.translation.provider.libretranslate.server.setup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.theme.TachiyomiTheme
import kotlinx.coroutines.launch
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.server.LibreTranslateServerConfiguration
import mihon.translation.provider.libretranslate.server.LibreTranslateServerNetwork
import mihon.translation.provider.libretranslate.setup.components.ProviderInformationCard
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupHeader
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupPrimaryButton
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupScaffold
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupStatus
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupStatusPanel

internal class LibreTranslateServerSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configuration = LibreTranslateServerConfiguration(applicationContext)
        val coordinator = LibreTranslateServerSetupCoordinator(
            serviceFactory = { endpoint, apiKey ->
                LibreTranslateHttpClient(
                    httpClient = LibreTranslateServerNetwork.httpClient,
                    endpoint = endpoint,
                    apiKey = apiKey,
                )
            },
            saveConfiguration = configuration::save,
        )
        setContent {
            TachiyomiTheme {
                LibreTranslateServerSetupRoute(
                    initialEndpoint = configuration.endpoint?.toString().orEmpty(),
                    initialApiKey = configuration.apiKey.orEmpty(),
                    coordinator = coordinator,
                    onBack = ::finish,
                    onDone = ::finish,
                )
            }
        }
    }
}

@Composable
private fun LibreTranslateServerSetupRoute(
    initialEndpoint: String,
    initialApiKey: String,
    coordinator: LibreTranslateServerSetupCoordinator,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var endpoint by rememberSaveable { mutableStateOf(initialEndpoint) }
    var apiKey by rememberSaveable { mutableStateOf(initialApiKey) }
    var endpointInvalid by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<ProviderSetupStatus?>(null) }
    val scope = rememberCoroutineScope()
    val testing = status == ProviderSetupStatus.Testing
    val readyMessage = stringResource(R.string.libretranslate_server_ready)
    val connectionFailureMessage = stringResource(R.string.libretranslate_server_connection_failed)
    val saveFailureMessage = stringResource(R.string.libretranslate_server_save_failed)

    LibreTranslateServerSetupScreen(
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
            if (LibreTranslateServerConfiguration.validateEndpoint(endpoint) == null) {
                endpointInvalid = true
                return@LibreTranslateServerSetupScreen
            }
            status = ProviderSetupStatus.Testing
            scope.launch {
                status = when (coordinator.saveAndTest(endpoint, apiKey)) {
                    LibreTranslateServerSetupResult.InvalidEndpoint -> {
                        endpointInvalid = true
                        null
                    }
                    LibreTranslateServerSetupResult.Ready ->
                        ProviderSetupStatus.Success(readyMessage)
                    LibreTranslateServerSetupResult.ConnectionFailed ->
                        ProviderSetupStatus.Failure(connectionFailureMessage)
                    LibreTranslateServerSetupResult.SaveFailed ->
                        ProviderSetupStatus.Failure(saveFailureMessage)
                }
            }
        },
        onBack = onBack,
        onDone = onDone,
    )
}

@Composable
private fun LibreTranslateServerSetupScreen(
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
        title = stringResource(R.string.libretranslate_server_setup_title),
        backContentDescription = stringResource(R.string.translation_provider_back),
        onBack = onBack,
    ) {
        ProviderSetupHeader(
            artworkResourceId = R.drawable.ic_libretranslate_server,
            title = stringResource(R.string.libretranslate_server_setup_title),
            description = stringResource(R.string.libretranslate_server_setup_description),
        )
        ProviderInformationCard(
            title = stringResource(R.string.libretranslate_server_security_title),
            lines = listOf(stringResource(R.string.libretranslate_server_security_description)),
        )
        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpointChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = { Text(stringResource(R.string.libretranslate_server_endpoint)) },
            supportingText = if (endpointInvalid) {
                {
                    Text(stringResource(R.string.libretranslate_server_invalid_endpoint))
                }
            } else {
                null
            },
            isError = endpointInvalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = { Text(stringResource(R.string.libretranslate_server_api_key)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (apiKeyVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(
                    onClick = { apiKeyVisible = !apiKeyVisible },
                    enabled = !testing,
                ) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
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
                label = stringResource(R.string.libretranslate_server_save_and_test),
                enabled = !testing,
                onClick = onTest,
            )
        }
    }
}

@Preview(name = "LibreTranslate setup states", showBackground = true)
@Composable
private fun LibreTranslateServerSetupPreview(
    @PreviewParameter(ServerSetupPreviewStateProvider::class) status: ProviderSetupStatus?,
) {
    TachiyomiPreviewTheme {
        LibreTranslateServerSetupScreen(
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
