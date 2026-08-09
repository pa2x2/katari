package mihon.translation.runtime

import mihon.feature.graph.validation.FeatureContractFailure
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineSelection
import mihon.translation.api.preparation.TranslationEngineChoiceReason
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.request.TranslationRequest
import mihon.translation.api.request.TranslationSourceLanguageSelection
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.runtime.feature.DefaultTranslationFeature
import mihon.translation.runtime.feature.TranslationDefaultTargetLanguageResolver
import mihon.translation.runtime.graph.TRANSLATION_FEATURE_ID
import mihon.translation.runtime.graph.TranslationFeatureBehaviorContract
import mihon.translation.runtime.graph.TranslationFeatureContributor
import mihon.translation.runtime.registry.DefaultTranslationEngineRegistry

class TranslationFeatureContractValidationContributor : FeatureValidationContributor {
    override val owner = TranslationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                contract = FeatureContractReference(
                    feature = TRANSLATION_FEATURE_ID,
                    contract = TranslationFeatureBehaviorContract,
                ),
            ) {
                val registry = DefaultTranslationEngineRegistry(emptyList())
                val feature = DefaultTranslationFeature(
                    engineRegistry = registry,
                    knownEngineCatalog = registry,
                    textLanguageDetectors = emptyList(),
                    defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
                    selectedEngine = { TranslationEngineId("missing") },
                )
                val preparation = feature.prepare(
                    TranslationRequest(
                        text = "Feature contract",
                        sourceLanguage = TranslationSourceLanguageSelection.Explicit(
                            LanguageTag.require("en"),
                        ),
                        targetLanguage = TranslationTargetLanguageSelection.Explicit(
                            LanguageTag.require("pl"),
                        ),
                        engine = TranslationEngineSelection.ProfileDefault,
                    ),
                )
                if (
                    preparation == TranslationPreparation.EngineChoiceRequired(
                        reason = TranslationEngineChoiceReason.SelectedEngineUnavailable(
                            TranslationEngineId("missing"),
                        ),
                        engines = emptyList(),
                    )
                ) {
                    FeatureContractVerificationResult.Passed
                } else {
                    FeatureContractVerificationResult.Failed(
                        listOf(
                            FeatureContractFailure(
                                "Translation runtime must preserve an unavailable explicit engine, " +
                                    "but returned $preparation",
                            ),
                        ),
                    )
                }
            },
        )
    }
}
