package mihon.translation.provider.libretranslate.server

import android.content.Context
import android.content.Intent
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.model.TranslationModelId
import mihon.translation.api.model.TranslationModelOperationResult
import mihon.translation.api.provider.TranslationProviderDisclosure
import mihon.translation.provider.libretranslate.server.setup.LibreTranslateServerSetupActivity
import mihon.translation.spi.setup.TranslationEngineSetup
import mihon.translation.spi.setup.TranslationSetupResult

internal class LibreTranslateServerSetup(
    private val context: Context,
    private val configuration: LibreTranslateServerConfiguration,
    private val openInAppSetup: () -> Boolean = {
        runCatching {
            context.startActivity(
                Intent(context, LibreTranslateServerSetupActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
    },
) : TranslationEngineSetup {
    override val engine = LibreTranslateServerEngine.ENGINE_ID
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == LibreTranslateServerEngine.DISCLOSURE)
        configuration.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult {
        return if (openInAppSetup()) {
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
