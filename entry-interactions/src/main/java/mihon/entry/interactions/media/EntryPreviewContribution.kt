package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.entrySourceContextInputDefinition
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_PREVIEW_FEATURE_ID = FeatureId("entry.preview")
private val ENTRY_PREVIEW_FEATURE_OWNER = ContributionOwner("entry-preview")
private val ENTRY_PREVIEW_REFERENCE = entryContentTypeReferenceContribution(
    id = "preview",
    owner = ENTRY_PREVIEW_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Preview entries while browsing",
    order = 100,
) { input ->
    when (input.requireMatchedProvider<EntryPreviewProcessor>().sourceRequirement) {
        EntryPreviewSourceRequirement.NONE -> EntryContentTypeReferenceStatus.SUPPORTED
        EntryPreviewSourceRequirement.PREVIEW_CAPABILITY -> EntryContentTypeReferenceStatus.SOURCE_DEPENDENT
    }
}
internal val ENTRY_PREVIEW_PROVIDER_INTEGRATION_ID = FeatureIntegrationId("entry.preview.provider")
internal val ENTRY_PREVIEW_CONTEXT_INTEGRATION_ID = FeatureIntegrationId("entry.preview.context")
internal val ENTRY_PREVIEW_CONFIGURATION_INTEGRATION_ID = FeatureIntegrationId("entry.preview.configuration")
internal val ENTRY_PREVIEW_CHILD_INTEGRATION_ID = FeatureIntegrationId("entry.preview.first-reading-child")
internal val ENTRY_PREVIEW_OPEN_INTEGRATION_ID = FeatureIntegrationId("entry.preview.open-target")

internal enum class EntryPreviewBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.preview.availability")),
    LOAD(FeatureArtifactId("entry.preview.load")),
    PAGE_LOAD(FeatureArtifactId("entry.preview.page-load")),
    LIFECYCLE(FeatureArtifactId("entry.preview.lifecycle")),
    ENTRY_SURFACE(FeatureArtifactId("entry.preview.entry-surface")),
    BROWSE_SURFACE(FeatureArtifactId("entry.preview.browse-surface")),
}

internal object EntryPreviewConfigurationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.preview.configuration.settings")
}

internal object EntryPreviewChildBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.preview.first-reading-child.selection")
}

internal object EntryPreviewOpenBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.preview.open-target.dispatch")
}

internal object EntryPreviewBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.preview.behavior")
}

internal object EntryPreviewProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.preview.provider-behavior")
}

internal object EntryPreviewConfigurationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.preview.configuration-behavior")
}

internal object EntryPreviewChildBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.preview.first-reading-child-behavior")
}

internal object EntryPreviewOpenBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.preview.open-target-behavior")
}

internal object EntryPreviewProviderDispatchBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.preview.provider-dispatch")
}

internal val ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT = contextInputDefinition<EntryPreviewSourceRequirement>(
    ContextInputId("entry.preview.source-requirement"),
    ContributionOwner("entry-preview-provider"),
)
internal val ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.preview.source-support"),
    contracts = setOf(EntryPreviewSource::class),
)
internal val ENTRY_PREVIEW_ENABLED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.preview.enabled"),
    ContributionOwner("entry-preview-configuration"),
)
private val ENTRY_PREVIEW_SOURCE_UNSUPPORTED = FeatureContextBlocker(
    FeatureArtifactId("entry.preview.source-unsupported"),
    listOf(ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT, ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT),
)
private val ENTRY_PREVIEW_DISABLED = FeatureContextBlocker(
    FeatureArtifactId("entry.preview.disabled"),
    listOf(ENTRY_PREVIEW_ENABLED_CONTEXT),
)

internal object EntryPreviewFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_PREVIEW_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_PREVIEW_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_PREVIEW_PROVIDER_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryPreviewCapability.definition),
                        behaviorProjections = listOf(EntryPreviewProviderDispatchBehavior),
                        behavioralContracts = listOf(EntryPreviewProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_PREVIEW_REFERENCE.requirement),
                        projections = listOf(ENTRY_PREVIEW_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PREVIEW_CONTEXT_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryPreviewCapability.definition),
                        contextInputs = listOf(
                            ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT,
                            ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT,
                            ENTRY_PREVIEW_ENABLED_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            val sourceRequired = evidence.value(ENTRY_PREVIEW_SOURCE_REQUIREMENT_CONTEXT) ==
                                EntryPreviewSourceRequirement.PREVIEW_CAPABILITY
                            when {
                                !evidence.value(ENTRY_PREVIEW_ENABLED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_PREVIEW_DISABLED))
                                sourceRequired && !evidence.value(ENTRY_PREVIEW_SOURCE_SUPPORT_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_PREVIEW_SOURCE_UNSUPPORTED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(ENTRY_PREVIEW_SOURCE_UNSUPPORTED, ENTRY_PREVIEW_DISABLED),
                        behaviorProjections = EntryPreviewBehavior.entries,
                        behavioralContracts = listOf(EntryPreviewBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PREVIEW_CONFIGURATION_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryPreviewCapability.definition),
                            CapabilityExpression.Provided(EntryPreviewConfigurationCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryPreviewConfigurationBehavior),
                        behavioralContracts = listOf(EntryPreviewConfigurationBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PREVIEW_CHILD_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryPreviewCapability.definition),
                            CapabilityExpression.Provided(EntryChildListCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryPreviewChildBehavior),
                        behavioralContracts = listOf(EntryPreviewChildBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_PREVIEW_OPEN_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryPreviewCapability.definition),
                            CapabilityExpression.Provided(EntryOpenCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryPreviewOpenBehavior),
                        behavioralContracts = listOf(EntryPreviewOpenBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
