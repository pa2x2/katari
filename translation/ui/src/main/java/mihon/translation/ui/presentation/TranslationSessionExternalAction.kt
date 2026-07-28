package mihon.translation.ui.presentation

import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationModelDescriptor
import mihon.translation.api.TranslationProviderDisclosure

sealed interface TranslationSessionExternalAction {
    data object ChooseSourceLanguage : TranslationSessionExternalAction

    data object ChooseTargetLanguage : TranslationSessionExternalAction

    data object ChooseEngine : TranslationSessionExternalAction

    data class ChangeLanguages(
        val source: TranslationLanguageTag,
        val target: TranslationLanguageTag,
    ) : TranslationSessionExternalAction

    data class ConfirmProviderDisclosure(
        val engine: TranslationEngineId,
        val disclosure: TranslationProviderDisclosure,
    ) : TranslationSessionExternalAction

    data class DownloadModels(
        val engine: TranslationEngineId,
        val models: List<TranslationModelDescriptor>,
    ) : TranslationSessionExternalAction

    data class OpenSystemSetup(
        val engine: TranslationEngineId,
    ) : TranslationSessionExternalAction

    data class OpenDocumentation(
        val url: String,
    ) : TranslationSessionExternalAction
}
