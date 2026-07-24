package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
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

internal val ENTRY_CHILD_GROUP_FILTER_FEATURE_ID = FeatureId("entry.child-group-filter")
internal val ENTRY_CHILD_GROUP_FILTER_OWNER = ContributionOwner("entry-child-group-filter")
private val ENTRY_CHILD_GROUP_FILTER_REFERENCE = entryContentTypeReferenceContribution(
    id = "child-release-group-filter",
    owner = ENTRY_CHILD_GROUP_FILTER_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Filter child items by release group",
    order = 800,
)
internal val ENTRY_CHILD_GROUP_FILTER_PROVIDER_INTEGRATION = FeatureIntegrationId("entry.child-group-filter.provider")

internal object EntryChildGroupFilterBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.child-group-filter.behavior")
}

private enum class EntryChildGroupFilterBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    STATE(FeatureArtifactId("entry.child-group-filter.state")),
    APPLY(FeatureArtifactId("entry.child-group-filter.apply")),
    PERSISTENCE(FeatureArtifactId("entry.child-group-filter.persistence")),
    BACKUP(FeatureArtifactId("entry.child-group-filter.backup")),
    SHARED_STORAGE(FeatureArtifactId("entry.child-group-filter.shared-storage-consumers")),
}

internal object EntryChildGroupFilterFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_CHILD_GROUP_FILTER_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_CHILD_GROUP_FILTER_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_CHILD_GROUP_FILTER_PROVIDER_INTEGRATION,
                        prerequisites = CapabilityExpression.Provided(EntryChildGroupFilterCapability.definition),
                        behaviorProjections = EntryChildGroupFilterBehavior.entries,
                        behavioralContracts = listOf(EntryChildGroupFilterBehaviorContract),
                        projectionRequirements = listOf(ENTRY_CHILD_GROUP_FILTER_REFERENCE.requirement),
                        projections = listOf(ENTRY_CHILD_GROUP_FILTER_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal fun FeatureGraphEvaluation.childGroupFilterProviderTypes() =
    EntryChildGroupFilterBehavior.entries
        .mapTo(mutableSetOf()) { behavior ->
            applicableProviderTypes<EntryChildGroupFilterProcessor>(
                feature = ENTRY_CHILD_GROUP_FILTER_FEATURE_ID,
                integration = ENTRY_CHILD_GROUP_FILTER_PROVIDER_INTEGRATION,
                behaviorProjection = behavior.id,
            )
        }
        .singleOrNull()
        ?: error("Child-group filtering behaviors selected different provider sets")
