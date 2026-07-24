package mihon.entry.interactions

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

internal val ENTRY_TYPE_PRESENTATION_FEATURE_ID = FeatureId("entry.type-presentation")
private val ENTRY_TYPE_PRESENTATION_FEATURE_OWNER = ContributionOwner("entry-type-presentation")
private val ENTRY_TYPE_PRESENTATION_REFERENCE = entryContentTypeReferenceContribution(
    id = "type-presentation",
    owner = ENTRY_TYPE_PRESENTATION_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Use media-specific terminology and presentation",
    order = 1300,
)
internal val ENTRY_TYPE_PRESENTATION_INTEGRATION_ID = FeatureIntegrationId("entry.type-presentation.provider")
private val ENTRY_TYPE_PRESENTATION_BEHAVIOR_ID = FeatureArtifactId("entry.type-presentation.vocabulary")

private object EntryTypePresentationBehavior : FeatureBehaviorProjection {
    override val id = ENTRY_TYPE_PRESENTATION_BEHAVIOR_ID
}

internal object EntryTypePresentationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.type-presentation.behavior")
}

internal object EntryTypePresentationFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_TYPE_PRESENTATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_TYPE_PRESENTATION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_TYPE_PRESENTATION_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryTypePresentationCapability.definition),
                        behaviorProjections = listOf(EntryTypePresentationBehavior),
                        behavioralContracts = listOf(EntryTypePresentationBehaviorContract),
                        projectionRequirements = listOf(ENTRY_TYPE_PRESENTATION_REFERENCE.requirement),
                        projections = listOf(ENTRY_TYPE_PRESENTATION_REFERENCE.projection),
                    ),
                ),
            ),
        )
    }
}

internal class DefaultEntryTypePresentationFeature(
    evaluation: FeatureGraphEvaluation,
    private val interaction: EntryTypePresentationInteraction,
) : EntryTypePresentationFeature {
    private val applicableTypes = evaluation.applicableProviderTypes<EntryTypePresentationProvider>(
        feature = ENTRY_TYPE_PRESENTATION_FEATURE_ID,
        integration = ENTRY_TYPE_PRESENTATION_INTEGRATION_ID,
        behaviorProjection = ENTRY_TYPE_PRESENTATION_BEHAVIOR_ID,
    )

    override val genericPresentation: EntryTypePresentation = genericEntryTypePresentation

    override fun presentation(type: EntryType?): EntryTypePresentationResult {
        if (type == null || type !in applicableTypes) {
            return EntryTypePresentationResult.Generic(type, genericPresentation)
        }
        return EntryTypePresentationResult.Contributed(
            type = type,
            presentation = checkNotNull(interaction.presentation(type)) {
                "Type-presentation graph selected $type without an operational provider"
            },
        )
    }
}
