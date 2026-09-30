package mihon.translation.provider.server.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.model.TranslationModelOperationResult
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.provider.server.ServerConnectionSettings
import mihon.translation.spi.setup.TranslationEngineSetup
import mihon.translation.spi.setup.TranslationSetupResult

/**
 * Sets up an engine that translates through a server of the user's own: the connection is configured in
 * [setupActivity], and accepting the [disclosure] is kept with the connection.
 */
class ServerEngineSetup(
    private val context: Context,
    private val settings: ServerConnectionSettings,
    override val engine: TranslationEngineId,
    private val disclosure: TranslationProviderDisclosure,
    private val setupActivity: Class<out Activity>,
) : TranslationEngineSetup {
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == this.disclosure)
        settings.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult {
        val opened = runCatching {
            context.startActivity(Intent(context, setupActivity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
        return if (opened) {
            TranslationSetupResult.Opened(TranslationSetupDestination.InApp)
        } else {
            TranslationSetupResult.SettingsUnavailable
        }
    }

    override suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ) = TranslationModelOperationResult.Failed("Language models are managed by the server")
}
