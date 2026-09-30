package mihon.translation.provider.deepl

import android.content.Context
import android.content.Intent
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.model.TranslationModelOperationResult
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.provider.deepl.setup.DeepLServerSetupActivity
import mihon.translation.provider.server.ServerConnectionConfiguration
import mihon.translation.spi.setup.TranslationEngineSetup
import mihon.translation.spi.setup.TranslationSetupResult

internal class DeepLServerSetup(
    private val context: Context,
    private val configuration: ServerConnectionConfiguration,
) : TranslationEngineSetup {
    override val engine = DeepLServerEngine.ENGINE_ID
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == DeepLServerEngine.DISCLOSURE)
        configuration.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult {
        val opened = runCatching {
            context.startActivity(
                Intent(context, DeepLServerSetupActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
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
