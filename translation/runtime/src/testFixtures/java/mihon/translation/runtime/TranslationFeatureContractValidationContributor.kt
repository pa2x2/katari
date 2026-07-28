package mihon.translation.runtime

import mihon.feature.graph.validation.FeatureContractFailure
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerificationResult
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationPreparation
import mihon.translation.api.TranslationRequest
import mihon.translation.api.TranslationSourceLanguageSelection
import mihon.translation.api.TranslationTargetLanguageSelection

class TranslationFeatureContractValidationContributor : FeatureValidationContributor {
    override val owner = TranslationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                contract = FeatureContractReference(
                    feature = TRANSLATION_FEATURE_ID,
                    contract = TranslationFeatureBehaviorContract,
                ),
            ) { input ->
                val registry = input.provider(TranslationEngineRegistryCapability.definition)
                val catalog = DefaultTranslationEngineRegistry(registry.engines)
                val feature = DefaultTranslationFeature(
                    engineRegistry = registry,
                    knownEngineCatalog = catalog,
                    sourceLanguageDetectors = emptyList(),
                    defaultTargetLanguageResolver = TranslationDefaultTargetLanguageResolver { null },
                )
                val preparation = feature.prepare(
                    TranslationRequest(
                        text = "Feature contract",
                        sourceLanguage = TranslationSourceLanguageSelection.Explicit(
                            TranslationLanguageTag.require("en"),
                        ),
                        targetLanguage = TranslationTargetLanguageSelection.Explicit(
                            TranslationLanguageTag.require("pl"),
                        ),
                        engine = TranslationEngineSelection.Automatic,
                    ),
                )
                if (preparation == TranslationPreparation.Unavailable(
                        mihon.translation.api.TranslationUnavailableReason.NoEngineAvailable,
                    )
                ) {
                    FeatureContractVerificationResult.Passed
                } else {
                    FeatureContractVerificationResult.Failed(
                        listOf(
                            FeatureContractFailure(
                                "Translation runtime without installed engines must report NoEngineAvailable, " +
                                    "but returned $preparation",
                            ),
                        ),
                    )
                }
            },
        )
    }
}
