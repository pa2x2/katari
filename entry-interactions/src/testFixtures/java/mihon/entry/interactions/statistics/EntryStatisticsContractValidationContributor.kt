package mihon.entry.interactions.statistics

import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.validation.contractExpectation
import mihon.entry.interactions.validation.productionSubjectEvaluation
import mihon.entry.interactions.validation.verifyFeatureContract
import mihon.feature.graph.validation.FeatureContractReference
import mihon.feature.graph.validation.FeatureContractVerifier
import mihon.feature.graph.validation.FeatureValidationContributionSink
import mihon.feature.graph.validation.FeatureValidationContributor

class EntryStatisticsContractValidationContributor : FeatureValidationContributor {
    override val owner = EntryStatisticsFeatureContributor.owner

    override fun contributeTo(sink: FeatureValidationContributionSink) {
        sink.add(
            FeatureContractVerifier(
                FeatureContractReference(
                    ENTRY_STATISTICS_FEATURE_ID,
                    EntryStatisticsBehaviorContract,
                ),
            ) { input ->
                verifyFeatureContract {
                    val statisticsProvider = input.provider(EntryStatisticsCapability.definition)
                    val presentationProvider = input.provider(EntryTypePresentationCapability.definition)
                    val evaluation = productionSubjectEvaluation(
                        listOf(
                            EntryStatisticsCapability.bind(statisticsProvider),
                            EntryTypePresentationCapability.bind(presentationProvider),
                        ),
                        EntryStatisticsFeatureContributor,
                    )
                    val contribution = EntryStatisticsContribution(
                        type = statisticsProvider.type,
                        accent = statisticsProvider.accent,
                        consumedUnitLabel = statisticsProvider.consumedUnitLabel,
                    )
                    val feature = DefaultEntryStatisticsFeature(
                        evaluation = evaluation,
                        interaction = object : EntryStatisticsInteraction {
                            override val contributions = listOf(contribution)
                        },
                    )

                    contractExpectation(
                        feature.contributions == listOf(contribution),
                        "Statistics must project the selected provider contribution",
                    )
                    contractExpectation(
                        feature.contribution(statisticsProvider.type) == contribution,
                        "Statistics must resolve the selected provider contribution by type",
                    )
                }
            },
        )
    }
}
