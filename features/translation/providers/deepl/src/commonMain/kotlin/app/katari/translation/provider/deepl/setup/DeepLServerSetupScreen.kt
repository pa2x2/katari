package app.katari.translation.provider.deepl.setup

import androidx.compose.runtime.Composable
import app.katari.translation.provider.deepl.resources.Res
import app.katari.translation.provider.deepl.resources.ic_deepl_compatible_server
import app.katari.translation.provider.server.setup.ProviderSetupScreen
import app.katari.translation.provider.server.setup.ServerEndpointSuggestion
import app.katari.translation.provider.server.setup.ServerSetupRoute
import app.katari.translation.provider.server.setup.ServerSetupTexts
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.painterResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.deepl_server_api_key
import tachiyomi.i18n.deepl_server_connection_failed
import tachiyomi.i18n.deepl_server_setup_description
import tachiyomi.i18n.deepl_server_setup_title
import tachiyomi.i18n.deepl_server_suggestion_free
import tachiyomi.i18n.deepl_server_suggestion_pro
import tachiyomi.presentation.core.i18n.stringResource

/** Where the user connects Katari to DeepL or to a server of their own that speaks the DeepL API. */
class DeepLServerSetupScreen : ProviderSetupScreen() {
    @Composable
    override fun Setup(onClose: () -> Unit) {
        val model = metroViewModel<DeepLServerSetupViewModel>()
        ServerSetupRoute(
            initialEndpoint = model.initialEndpoint,
            initialApiKey = model.initialApiKey,
            coordinator = model.coordinator,
            texts = ServerSetupTexts(
                title = stringResource(MR.strings.deepl_server_setup_title),
                description = stringResource(MR.strings.deepl_server_setup_description),
                apiKeyLabel = stringResource(MR.strings.deepl_server_api_key),
                connectionFailed = stringResource(MR.strings.deepl_server_connection_failed),
            ),
            securityDescription = stringResource(model.keyStorage),
            artwork = painterResource(Res.drawable.ic_deepl_compatible_server),
            onBack = onClose,
            onDone = onClose,
            endpointSuggestions = listOf(
                ServerEndpointSuggestion(
                    label = stringResource(MR.strings.deepl_server_suggestion_free),
                    endpoint = "https://api-free.deepl.com/",
                ),
                ServerEndpointSuggestion(
                    label = stringResource(MR.strings.deepl_server_suggestion_pro),
                    endpoint = "https://api.deepl.com/",
                ),
            ),
        )
    }
}
