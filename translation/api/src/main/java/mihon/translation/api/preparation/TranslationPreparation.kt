package mihon.translation.api

/** Opaque, process-local execution authority returned only by [TranslationFeature.prepare]. */
interface ReadyTranslation

sealed interface TranslationPreparation {
    data class Ready(
        val translation: ReadyTranslation,
        val request: ResolvedTranslationRequest,
        val presentation: TranslationProviderPresentation,
    ) : TranslationPreparation

    data class ProviderDisclosureRequired(
        val engine: TranslationEngineId,
        val presentation: TranslationProviderPresentation,
        val disclosure: TranslationProviderDisclosure,
    ) : TranslationPreparation

    data class ModelDownloadRequired(
        val engine: TranslationEngineId,
        val presentation: TranslationProviderPresentation,
        val models: List<TranslationModelDescriptor>,
    ) : TranslationPreparation {
        init {
            require(models.isNotEmpty())
        }
    }

    data class SystemSetupRequired(
        val engine: TranslationEngineId,
        val presentation: TranslationProviderPresentation,
        val reason: TranslationSystemSetupReason,
    ) : TranslationPreparation

    data class SetupInProgress(
        val engine: TranslationEngineId,
        val presentation: TranslationProviderPresentation,
        val progress: TranslationOperationProgress? = null,
    ) : TranslationPreparation

    data class SourceUndetermined(
        val suggestedLanguages: List<TranslationLanguageTag> = emptyList(),
    ) : TranslationPreparation

    data class TargetLanguageRequired(
        val sourceLanguage: TranslationLanguageTag?,
        val reason: TranslationTargetChoiceReason,
    ) : TranslationPreparation

    data class EngineChoiceRequired(
        val reason: TranslationEngineChoiceReason,
        val engines: List<KnownTranslationEngine>,
    ) : TranslationPreparation

    data class Unavailable(
        val reason: TranslationUnavailableReason,
    ) : TranslationPreparation

    data class Rejected(
        val reason: TranslationRejectionReason,
    ) : TranslationPreparation
}

sealed interface TranslationSystemSetupReason {
    data object ServiceDisabled : TranslationSystemSetupReason

    data object LanguageModelsRequired : TranslationSystemSetupReason

    data class ProviderActionRequired(
        val description: String,
    ) : TranslationSystemSetupReason {
        init {
            require(description.isNotBlank())
        }
    }
}

sealed interface TranslationTargetChoiceReason {
    data object NoDefaultTarget : TranslationTargetChoiceReason

    data object SourceEqualsTarget : TranslationTargetChoiceReason
}

sealed interface TranslationEngineChoiceReason {
    data object NoSelection : TranslationEngineChoiceReason

    data class SelectedEngineUnavailable(
        val engine: TranslationEngineId,
    ) : TranslationEngineChoiceReason
}

sealed interface TranslationUnavailableReason {
    data class UnsupportedOs(
        val minimumApi: Int,
    ) : TranslationUnavailableReason

    data object ServiceMissing : TranslationUnavailableReason

    data object SystemSettingsUnavailable : TranslationUnavailableReason

    data class UnsupportedLanguage(
        val language: TranslationLanguageTag,
    ) : TranslationUnavailableReason

    data class UnsupportedLanguagePair(
        val source: TranslationLanguageTag,
        val target: TranslationLanguageTag,
    ) : TranslationUnavailableReason

    data class EngineUnavailable(
        val engine: TranslationEngineId,
        val reason: String,
    ) : TranslationUnavailableReason {
        init {
            require(reason.isNotBlank())
        }
    }

    data object NoEngineAvailable : TranslationUnavailableReason
}

sealed interface TranslationRejectionReason {
    data object BlankInput : TranslationRejectionReason

    data class InputTooLarge(
        val actualCodePoints: Int,
        val maximumCodePoints: Int,
    ) : TranslationRejectionReason {
        init {
            require(actualCodePoints > maximumCodePoints)
            require(maximumCodePoints > 0)
        }
    }
}
