package app.katari.translation.ui.presentation

import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.model.TranslationModelDescriptor
import app.katari.translation.api.provider.TranslationProviderDisclosure

sealed interface TranslationSessionExternalAction {
    data object ChooseSourceLanguage : TranslationSessionExternalAction

    data object ChooseTargetLanguage : TranslationSessionExternalAction

    data object ChooseEngine : TranslationSessionExternalAction

    data class ConfirmProviderDisclosure(
        val engine: TranslationEngineId,
        val disclosure: TranslationProviderDisclosure,
    ) : TranslationSessionExternalAction

    data class DownloadModels(
        val engine: TranslationEngineId,
        val models: List<TranslationModelDescriptor>,
    ) : TranslationSessionExternalAction

    data class OpenSetup(
        val engine: TranslationEngineId,
    ) : TranslationSessionExternalAction

    data class OpenDocumentation(
        val url: String,
    ) : TranslationSessionExternalAction
}
