package app.katari.translation.provider.libretranslate.server.setup

import androidx.compose.runtime.Composable
import app.katari.translation.provider.libretranslate.resources.Res
import app.katari.translation.provider.libretranslate.resources.ic_libretranslate_server
import app.katari.translation.provider.server.setup.ProviderSetupScreen
import app.katari.translation.provider.server.setup.ServerSetupRoute
import app.katari.translation.provider.server.setup.ServerSetupTexts
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.painterResource
import tachiyomi.i18n.MR
import tachiyomi.i18n.libretranslate_server_api_key
import tachiyomi.i18n.libretranslate_server_connection_failed
import tachiyomi.i18n.libretranslate_server_setup_description
import tachiyomi.i18n.libretranslate_server_setup_title
import tachiyomi.presentation.core.i18n.stringResource

/** Where the user connects Katari to a LibreTranslate-compatible server. */
data object LibreTranslateServerSetupScreen : ProviderSetupScreen() {
    @Composable
    override fun Setup(onClose: () -> Unit) {
        val model = metroViewModel<LibreTranslateServerSetupViewModel>()
        ServerSetupRoute(
            initialEndpoint = model.initialEndpoint,
            initialApiKey = model.initialApiKey,
            coordinator = model.coordinator,
            texts = ServerSetupTexts(
                title = stringResource(MR.strings.libretranslate_server_setup_title),
                description = stringResource(MR.strings.libretranslate_server_setup_description),
                apiKeyLabel = stringResource(MR.strings.libretranslate_server_api_key),
                connectionFailed = stringResource(MR.strings.libretranslate_server_connection_failed),
            ),
            securityDescription = stringResource(model.keyStorage),
            artwork = painterResource(Res.drawable.ic_libretranslate_server),
            onBack = onClose,
            onDone = onClose,
        )
    }
}
