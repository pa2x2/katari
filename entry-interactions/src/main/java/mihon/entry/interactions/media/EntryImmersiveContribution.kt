package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.SourceMetadata
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.supportedEntryTypes
import kotlinx.coroutines.CancellationException
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.EntryContentTypeReferenceStatus
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.source.ENTRY_SOURCE_DESCRIPTION_CONTEXT_OWNER
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
import tachiyomi.domain.entry.model.Entry

internal val ENTRY_IMMERSIVE_FEATURE_ID = FeatureId("entry.immersive")
private val ENTRY_IMMERSIVE_FEATURE_OWNER = ContributionOwner("entry-immersive")
private val ENTRY_IMMERSIVE_REFERENCE = entryContentTypeReferenceContribution(
    id = "immersive-browsing",
    owner = ENTRY_IMMERSIVE_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DISCOVERY_AND_INTEGRATIONS,
    label = "Use immersive browsing",
    order = 200,
    project = { EntryContentTypeReferenceStatus.SOURCE_DEPENDENT },
)
internal val ENTRY_IMMERSIVE_PROVIDER_INTEGRATION_ID = FeatureIntegrationId("entry.immersive.provider")
internal val ENTRY_IMMERSIVE_SOURCE_CONTEXT_INTEGRATION_ID = FeatureIntegrationId("entry.immersive.source-context")
internal val ENTRY_IMMERSIVE_ENTRY_CONTEXT_INTEGRATION_ID = FeatureIntegrationId("entry.immersive.entry-context")
internal val ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID = FeatureIntegrationId("entry.immersive.first-reading-child")
internal val ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID = FeatureIntegrationId("entry.immersive.open-target")

internal enum class EntryImmersiveBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    SOURCE_AVAILABILITY(FeatureArtifactId("entry.immersive.source-availability")),
    ENTRY_AVAILABILITY(FeatureArtifactId("entry.immersive.entry-availability")),
    CATALOGUE_SURFACE(FeatureArtifactId("entry.immersive.catalogue-surface")),
    FEED_SURFACE(FeatureArtifactId("entry.immersive.feed-surface")),
    LONG_PRESS(FeatureArtifactId("entry.immersive.long-press")),
    LOAD(FeatureArtifactId("entry.immersive.load")),
    PRELOAD(FeatureArtifactId("entry.immersive.preload")),
    RENDER(FeatureArtifactId("entry.immersive.render")),
    PROGRESS(FeatureArtifactId("entry.immersive.progress")),
    LIFECYCLE(FeatureArtifactId("entry.immersive.lifecycle")),
}

internal val ENTRY_IMMERSIVE_SOURCE_BEHAVIORS = setOf(
    EntryImmersiveBehavior.SOURCE_AVAILABILITY,
    EntryImmersiveBehavior.CATALOGUE_SURFACE,
    EntryImmersiveBehavior.FEED_SURFACE,
    EntryImmersiveBehavior.LONG_PRESS,
)
internal val ENTRY_IMMERSIVE_ENTRY_BEHAVIORS = setOf(
    EntryImmersiveBehavior.ENTRY_AVAILABILITY,
    EntryImmersiveBehavior.LOAD,
    EntryImmersiveBehavior.RENDER,
    EntryImmersiveBehavior.PROGRESS,
    EntryImmersiveBehavior.LIFECYCLE,
)

internal object EntryImmersiveProviderDispatchBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.immersive.provider-dispatch")
}

internal val ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.immersive.source-installed"),
    nonContractReason = "Source installation is runtime registration state, not a public SDK contract",
)
internal val ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.immersive.source-opt-in"),
    contracts = setOf(EntryCatalogueSource::class),
)
internal val ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT = entrySourceContextInputDefinition<Boolean>(
    id = ContextInputId("entry.immersive.declared-type-compatibility"),
    owner = ENTRY_SOURCE_DESCRIPTION_CONTEXT_OWNER,
    contracts = setOf(SourceMetadata::class),
)
private val ENTRY_IMMERSIVE_SOURCE_UNAVAILABLE = FeatureContextBlocker(
    FeatureArtifactId("entry.immersive.source-unavailable"),
    listOf(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT),
)
private val ENTRY_IMMERSIVE_SOURCE_OPTED_OUT = FeatureContextBlocker(
    FeatureArtifactId("entry.immersive.source-opted-out"),
    listOf(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT),
)
private val ENTRY_IMMERSIVE_DECLARED_TYPE_INCOMPATIBLE = FeatureContextBlocker(
    FeatureArtifactId("entry.immersive.declared-type-incompatible"),
    listOf(ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT),
)

internal object EntryImmersiveChildBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.immersive.first-reading-child.selection")
}

internal object EntryImmersiveChildRefreshBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.immersive.first-reading-child.refresh")
}

internal object EntryImmersiveOpenBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.immersive.open-target.dispatch")
}

internal object EntryImmersiveBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.immersive.behavior")
}

internal object EntryImmersiveProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.immersive.provider-behavior")
}

internal object EntryImmersiveSourceBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.immersive.source-context-behavior")
}

internal object EntryImmersiveChildBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.immersive.first-reading-child-behavior")
}

internal object EntryImmersiveOpenBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.immersive.open-target-behavior")
}

internal object EntryImmersiveFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_IMMERSIVE_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_IMMERSIVE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_IMMERSIVE_PROVIDER_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryImmersiveCapability.definition),
                        behaviorProjections = listOf(
                            EntryImmersiveProviderDispatchBehavior,
                            EntryImmersiveBehavior.PRELOAD,
                        ),
                        behavioralContracts = listOf(EntryImmersiveProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_IMMERSIVE_REFERENCE.requirement),
                        projections = listOf(ENTRY_IMMERSIVE_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_IMMERSIVE_SOURCE_CONTEXT_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryImmersiveCapability.definition),
                        contextInputs = listOf(
                            ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT,
                            ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT,
                            ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_IMMERSIVE_SOURCE_UNAVAILABLE))
                                !evidence.value(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_IMMERSIVE_SOURCE_OPTED_OUT))
                                !evidence.value(ENTRY_IMMERSIVE_DECLARED_COMPATIBILITY_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_IMMERSIVE_DECLARED_TYPE_INCOMPATIBLE))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            ENTRY_IMMERSIVE_SOURCE_UNAVAILABLE,
                            ENTRY_IMMERSIVE_SOURCE_OPTED_OUT,
                            ENTRY_IMMERSIVE_DECLARED_TYPE_INCOMPATIBLE,
                        ),
                        behaviorProjections = ENTRY_IMMERSIVE_SOURCE_BEHAVIORS.toList(),
                        behavioralContracts = listOf(EntryImmersiveSourceBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_IMMERSIVE_ENTRY_CONTEXT_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryImmersiveCapability.definition),
                        contextInputs = listOf(
                            ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT,
                            ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_IMMERSIVE_SOURCE_INSTALLED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_IMMERSIVE_SOURCE_UNAVAILABLE))
                                !evidence.value(ENTRY_IMMERSIVE_SOURCE_OPT_IN_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(ENTRY_IMMERSIVE_SOURCE_OPTED_OUT))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            ENTRY_IMMERSIVE_SOURCE_UNAVAILABLE,
                            ENTRY_IMMERSIVE_SOURCE_OPTED_OUT,
                        ),
                        behaviorProjections = ENTRY_IMMERSIVE_ENTRY_BEHAVIORS.toList(),
                        behavioralContracts = listOf(EntryImmersiveBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_IMMERSIVE_CHILD_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryImmersiveCapability.definition),
                            CapabilityExpression.Provided(EntryChildListCapability.definition),
                        ),
                        behaviorProjections = listOf(
                            EntryImmersiveChildBehavior,
                            EntryImmersiveChildRefreshBehavior,
                        ),
                        behavioralContracts = listOf(EntryImmersiveChildBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_IMMERSIVE_OPEN_INTEGRATION_ID,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryImmersiveCapability.definition),
                            CapabilityExpression.Provided(EntryOpenCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryImmersiveOpenBehavior),
                        behavioralContracts = listOf(EntryImmersiveOpenBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
