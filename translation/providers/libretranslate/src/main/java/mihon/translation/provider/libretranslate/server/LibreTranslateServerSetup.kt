package mihon.translation.provider.libretranslate.server

import android.content.Context
import android.content.Intent
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.provider.libretranslate.server.setup.LibreTranslateServerSetupActivity
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationSetupResult

internal class LibreTranslateServerSetup(
    private val context: Context,
    private val configuration: LibreTranslateServerConfiguration,
) : TranslationEngineSetup {
    override val engine = LibreTranslateServerEngine.ENGINE_ID
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == LibreTranslateServerEngine.DISCLOSURE)
        configuration.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult {
        return if (
            runCatching {
                context.startActivity(
                    Intent(context, LibreTranslateServerSetupActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.isSuccess
        ) {
            TranslationSetupResult.Opened
        } else {
            TranslationSetupResult.SettingsUnavailable
        }
    }

    override suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ) = TranslationModelOperationResult.Failed("Language models are managed by the server")
}
