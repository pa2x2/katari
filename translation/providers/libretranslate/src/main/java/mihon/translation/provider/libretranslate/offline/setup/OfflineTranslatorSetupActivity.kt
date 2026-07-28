package mihon.translation.provider.libretranslate.offline.setup

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import eu.kanade.presentation.theme.TachiyomiPreviewTheme
import eu.kanade.presentation.theme.TachiyomiTheme
import kotlinx.coroutines.launch
import mihon.translation.provider.libretranslate.R
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorApplication
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorConfiguration
import mihon.translation.provider.libretranslate.offline.OfflineTranslatorNetwork
import mihon.translation.provider.libretranslate.protocol.LibreTranslateHttpClient
import mihon.translation.provider.libretranslate.setup.components.ProviderInformationCard
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupHeader
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupPrimaryButton
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupScaffold
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupSecondaryButton
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupStatus
import mihon.translation.provider.libretranslate.setup.components.ProviderSetupStatusPanel

internal class OfflineTranslatorSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val configuration = OfflineTranslatorConfiguration(applicationContext)
        val providerApplication = OfflineTranslatorApplication(applicationContext)
        val coordinator = OfflineTranslatorSetupCoordinator(configuration) { endpoint ->
            LibreTranslateHttpClient(
                httpClient = OfflineTranslatorNetwork.httpClient,
                endpoint = endpoint,
            )
        }
        setContent {
            TachiyomiTheme {
                OfflineTranslatorSetupRoute(
                    initialPort = configuration.port.toString(),
                    coordinator = coordinator,
                    onOpenProvider = providerApplication::open,
                    onBack = ::finish,
                    onDone = ::finish,
                )
            }
        }
    }
}

@Composable
private fun OfflineTranslatorSetupRoute(
    initialPort: String,
    coordinator: OfflineTranslatorSetupCoordinator,
    onOpenProvider: () -> Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    var port by rememberSaveable { mutableStateOf(initialPort) }
    var portInvalid by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf<ProviderSetupStatus?>(null) }
    val scope = rememberCoroutineScope()
    val testing = status == ProviderSetupStatus.Testing
    val readyMessage = stringResource(R.string.offline_translator_ready)
    val connectionFailureMessage = stringResource(R.string.offline_translator_connection_failed)

    OfflineTranslatorSetupScreen(
        port = port,
        portInvalid = portInvalid,
        status = status,
        testing = testing,
        onPortChange = {
            port = it
            portInvalid = false
            status = null
        },
        onTest = {
            if (OfflineTranslatorConfiguration.parsePort(port) == null) {
                portInvalid = true
                return@OfflineTranslatorSetupScreen
            }
            status = ProviderSetupStatus.Testing
            scope.launch {
                status = when (coordinator.test(port)) {
                    OfflineTranslatorSetupResult.InvalidPort -> {
                        portInvalid = true
                        null
                    }
                    OfflineTranslatorSetupResult.Ready ->
                        ProviderSetupStatus.Success(readyMessage)
                    OfflineTranslatorSetupResult.ConnectionFailed ->
                        ProviderSetupStatus.Failure(connectionFailureMessage)
                }
            }
        },
        onOpenProvider = {
            if (!onOpenProvider()) {
                status = ProviderSetupStatus.Failure(connectionFailureMessage)
            }
        },
        onBack = onBack,
        onDone = onDone,
    )
}

@Composable
private fun OfflineTranslatorSetupScreen(
    port: String,
    portInvalid: Boolean,
    status: ProviderSetupStatus?,
    testing: Boolean,
    onPortChange: (String) -> Unit,
    onTest: () -> Unit,
    onOpenProvider: () -> Unit,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    ProviderSetupScaffold(
        title = stringResource(R.string.offline_translator_setup_title),
        backContentDescription = stringResource(R.string.translation_provider_back),
        onBack = onBack,
    ) {
        ProviderSetupHeader(
            artworkResourceId = R.drawable.ic_offline_translator,
            title = stringResource(R.string.offline_translator_setup_title),
            description = stringResource(R.string.offline_translator_setup_description),
        )
        ProviderInformationCard(
            lines = listOf(
                stringResource(R.string.offline_translator_step_models),
                stringResource(R.string.offline_translator_step_api),
                stringResource(R.string.offline_translator_step_port),
            ),
        )
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !testing,
            label = { Text(stringResource(R.string.offline_translator_port_label)) },
            supportingText = {
                Text(
                    stringResource(
                        if (portInvalid) {
                            R.string.offline_translator_invalid_port
                        } else {
                            R.string.offline_translator_port_supporting
                        },
                    ),
                )
            },
            isError = portInvalid,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                label = stringResource(R.string.offline_translator_save_and_test),
                enabled = !testing,
                onClick = onTest,
            )
        }
        ProviderSetupSecondaryButton(
            label = stringResource(R.string.offline_translator_open_app),
            enabled = !testing,
            onClick = onOpenProvider,
        )
    }
}

@Preview(name = "Offline setup states", showBackground = true)
@Preview(name = "Offline setup large font", showBackground = true, fontScale = 2f, heightDp = 1000)
@Composable
private fun OfflineTranslatorSetupPreview(
    @PreviewParameter(OfflineSetupPreviewStateProvider::class) status: ProviderSetupStatus?,
) {
    TachiyomiPreviewTheme {
        OfflineTranslatorSetupScreen(
            port = "5000",
            portInvalid = false,
            status = status,
            testing = status == ProviderSetupStatus.Testing,
            onPortChange = {},
            onTest = {},
            onOpenProvider = {},
            onBack = {},
            onDone = {},
        )
    }
}

private class OfflineSetupPreviewStateProvider : PreviewParameterProvider<ProviderSetupStatus?> {
    override val values = sequenceOf(
        null,
        ProviderSetupStatus.Testing,
        ProviderSetupStatus.Success("Ready to use"),
        ProviderSetupStatus.Failure("Enable the localhost API and download language models."),
    )
}
