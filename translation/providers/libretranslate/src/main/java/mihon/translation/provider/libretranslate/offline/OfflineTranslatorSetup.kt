package mihon.translation.provider.libretranslate.offline

import android.content.Context
import android.content.Intent
import mihon.translation.api.TranslationModelId
import mihon.translation.api.TranslationModelOperationResult
import mihon.translation.api.TranslationProviderDisclosure
import mihon.translation.api.TranslationSetupDestination
import mihon.translation.provider.libretranslate.offline.setup.OfflineTranslatorSetupActivity
import mihon.translation.spi.TranslationEngineSetup
import mihon.translation.spi.TranslationSetupResult

internal class OfflineTranslatorSetup(
    private val context: Context,
    private val application: OfflineTranslatorApp,
    private val settings: OfflineTranslatorSettings,
    private val openInAppSetup: () -> Boolean = {
        runCatching {
            context.startActivity(
                Intent(context, OfflineTranslatorSetupActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
    },
) : TranslationEngineSetup {
    override val engine = OfflineTranslatorEngine.ENGINE_ID
    override val supportsSetup = true

    override suspend fun acknowledge(disclosure: TranslationProviderDisclosure) {
        require(disclosure == OfflineTranslatorEngine.DISCLOSURE) {
            "Offline Translator cannot acknowledge another provider's disclosure"
        }
        settings.disclosureAccepted = true
    }

    override suspend fun openSetup(): TranslationSetupResult {
        val destination = if (application.isInstalled()) {
            TranslationSetupDestination.InApp
        } else {
            TranslationSetupDestination.External
        }
        val opened = when (destination) {
            TranslationSetupDestination.InApp -> openInAppSetup()
            TranslationSetupDestination.External -> application.openInstallationPage()
        }
        return if (opened) {
            TranslationSetupResult.Opened(destination)
        } else {
            TranslationSetupResult.SettingsUnavailable
        }
    }

    override suspend fun downloadModels(
        models: Set<TranslationModelId>,
        allowMeteredNetwork: Boolean,
    ): TranslationModelOperationResult {
        application.open()
        return TranslationModelOperationResult.Failed(
            "Offline Translator manages language models in its own settings",
        )
    }
}
