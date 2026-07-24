package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
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
import tachiyomi.domain.entry.model.Entry

internal val ENTRY_CONTINUE_FEATURE_ID = FeatureId("entry.continue")
internal val ENTRY_CONTINUE_INTEGRATION_ID = FeatureIntegrationId("entry.continue.provider")
private val ENTRY_CONTINUE_FEATURE_OWNER = ContributionOwner("entry-continue")
private val ENTRY_CONTINUE_REFERENCE = entryContentTypeReferenceContribution(
    id = "continue",
    owner = ENTRY_CONTINUE_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Continue from saved progress",
    order = 100,
)
private val ENTRY_CONTINUE_DISPATCH_BEHAVIOR_ID = FeatureArtifactId("entry.continue.dispatch")
private val ENTRY_CONTINUE_TARGET_BEHAVIOR_ID = FeatureArtifactId("entry.continue.next-target")

private object EntryContinueDispatchBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CONTINUE_DISPATCH_BEHAVIOR_ID
}

private object EntryContinueTargetBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_CONTINUE_TARGET_BEHAVIOR_ID
}

internal object EntryContinueBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.continue.behavior")
}

internal object EntryContinueFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_CONTINUE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_CONTINUE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_CONTINUE_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryContinueCapability.definition),
                        behaviorProjections = listOf(
                            EntryContinueDispatchBehavior,
                            EntryContinueTargetBehavior,
                        ),
                        behavioralContracts = listOf(EntryContinueBehaviorContract),
                        projectionRequirements = listOf(ENTRY_CONTINUE_REFERENCE.requirement),
                        projections = listOf(ENTRY_CONTINUE_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryContinueFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryContinueInteraction,
) : EntryContinueFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryContinueProcessor>(
        feature = ENTRY_CONTINUE_FEATURE_ID,
        integration = ENTRY_CONTINUE_INTEGRATION_ID,
        behaviorProjection = ENTRY_CONTINUE_DISPATCH_BEHAVIOR_ID,
    )
    private val targetTypes = evaluation.applicableProviderTypes<EntryContinueProcessor>(
        feature = ENTRY_CONTINUE_FEATURE_ID,
        integration = ENTRY_CONTINUE_INTEGRATION_ID,
        behaviorProjection = ENTRY_CONTINUE_TARGET_BEHAVIOR_ID,
    )

    init {
        check(applicableTypes == targetTypes) {
            "Continue dispatch and target behaviors selected different provider sets"
        }
    }

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun nextTarget(entry: Entry): EntryContinueTargetResult {
        if (entry.type !in targetTypes) return EntryContinueTargetResult.Inapplicable
        return interaction.findNext(entry)
            ?.let(EntryContinueTargetResult::Available)
            ?: EntryContinueTargetResult.NoNext
    }

    override suspend fun continueEntry(context: Context, entry: Entry): EntryContinueResult {
        if (!isApplicable(entry.type)) return EntryContinueResult.Inapplicable
        return interaction.continueEntry(context, entry)
            ?.let(EntryContinueResult::Opened)
            ?: EntryContinueResult.NoNext
    }
}
