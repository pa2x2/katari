package mihon.entry.interactions

import android.app.PendingIntent
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
import tachiyomi.domain.entry.model.EntryChapter

internal val ENTRY_OPEN_FEATURE_ID = FeatureId("entry.open")
internal val ENTRY_OPEN_INTEGRATION_ID = FeatureIntegrationId("entry.open.provider")
private val ENTRY_OPEN_FEATURE_OWNER = ContributionOwner("entry-open")
private val ENTRY_OPEN_REFERENCE = entryContentTypeReferenceContribution(
    id = "open",
    owner = ENTRY_OPEN_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Open entries in their media experience",
    order = 50,
)
private val ENTRY_OPEN_DISPATCH_BEHAVIOR_ID = FeatureArtifactId("entry.open.dispatch")

private object EntryOpenDispatchBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_OPEN_DISPATCH_BEHAVIOR_ID
}

internal object EntryOpenBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.open.behavior")
}

internal object EntryOpenFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_OPEN_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_OPEN_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_OPEN_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryOpenCapability.definition),
                        behaviorProjections = listOf(EntryOpenDispatchBehavior),
                        behavioralContracts = listOf(EntryOpenBehaviorContract),
                        projectionRequirements = listOf(ENTRY_OPEN_REFERENCE.requirement),
                        projections = listOf(ENTRY_OPEN_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryOpenFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryOpenInteraction,
) : EntryOpenFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryOpenProcessor>(
        feature = ENTRY_OPEN_FEATURE_ID,
        integration = ENTRY_OPEN_INTEGRATION_ID,
        behaviorProjection = ENTRY_OPEN_DISPATCH_BEHAVIOR_ID,
    )

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override fun open(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): Boolean {
        if (!isApplicable(entry.type)) return false
        interaction.open(context, entry, chapter, options)
        return true
    }

    override fun pendingIntent(
        context: Context,
        entry: Entry,
        chapter: EntryChapter,
        options: EntryOpenOptions,
    ): PendingIntent? {
        if (!isApplicable(entry.type)) return null
        return interaction.pendingIntent(context, entry, chapter, options)
    }
}
