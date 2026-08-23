package mihon.entry.interactions.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsCapability
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.runtime.EntryTypePresentationCapability
import mihon.entry.interactions.runtime.applicableProviderTypes
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf

internal val ENTRY_STATISTICS_FEATURE_ID = FeatureId("entry.statistics")
private val ENTRY_STATISTICS_FEATURE_OWNER = ContributionOwner("entry-statistics")
internal val ENTRY_STATISTICS_INTEGRATION_ID = FeatureIntegrationId("entry.statistics.provider")
private val ENTRY_STATISTICS_BEHAVIOR_ID = FeatureArtifactId("entry.statistics.contribution")

private object EntryStatisticsBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_STATISTICS_BEHAVIOR_ID
}

internal object EntryStatisticsBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.statistics.behavior")
}

internal object EntryStatisticsFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_STATISTICS_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_STATISTICS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_STATISTICS_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryStatisticsCapability.definition),
                            CapabilityExpression.Provided(EntryTypePresentationCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryStatisticsBehavior),
                        behavioralContracts = listOf(EntryStatisticsBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryStatisticsFeature(
    evaluation: FeatureGraphEvaluation,
    interaction: EntryStatisticsInteraction,
) : EntryStatisticsFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryStatisticsProvider>(
        feature = ENTRY_STATISTICS_FEATURE_ID,
        integration = ENTRY_STATISTICS_INTEGRATION_ID,
        behaviorProjection = ENTRY_STATISTICS_BEHAVIOR_ID,
    )

    override val contributions = interaction.contributions
        .filter { it.type in applicableTypes }
        .sortedBy { EntryType.entries.indexOf(it.type) }
    private val contributionsByType = contributions.associateBy(EntryStatisticsContribution::type)

    override fun contribution(type: EntryType): EntryStatisticsContribution? = contributionsByType[type]
}
