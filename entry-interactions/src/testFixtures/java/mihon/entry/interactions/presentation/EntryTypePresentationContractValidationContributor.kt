package mihon.entry.interactions

import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor

class EntryTypePresentationContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryTypePresentationFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_TYPE_PRESENTATION_FEATURE_ID,
                    EntryTypePresentationBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val provider = input.provider(EntryTypePresentationCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        EntryTypePresentationCapability.bind(provider),
                        EntryTypePresentationFeatureContributor,
                    )
                    val feature = DefaultEntryTypePresentationFeature(
                        evaluation = evaluation,
                        interaction = object : EntryTypePresentationInteraction {
                            override fun presentation(type: eu.kanade.tachiyomi.source.entry.EntryType) =
                                provider.presentation.takeIf { type == provider.type }
                        },
                    )

                    contractExpectation(
                        feature.presentation(provider.type) ==
                            EntryTypePresentationResult.Contributed(provider.type, provider.presentation),
                        "Type Presentation must project the selected vocabulary",
                    )
                    contractExpectation(
                        feature.genericPresentation == genericEntryTypePresentation,
                        "Type Presentation must retain neutral vocabulary",
                    )
                }
            },
        )
    }
}
