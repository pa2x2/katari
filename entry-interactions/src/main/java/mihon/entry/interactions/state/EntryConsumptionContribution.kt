package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
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
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_CONSUMPTION_FEATURE_ID = FeatureId("entry.consumption")
private val FEATURE_OWNER = ContributionOwner("entry-consumption")
private val ENTRY_CONSUMPTION_REFERENCE = entryContentTypeReferenceContribution(
    id = "consumption",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.ENTRY_INTERACTIONS,
    label = "Mark individual child items consumed or unconsumed",
    order = 200,
)
internal val ENTRY_CONSUMPTION_PROVIDER_INTEGRATION = FeatureIntegrationId("entry.consumption.provider")
internal val ENTRY_CONSUMPTION_ELIGIBILITY_INTEGRATION = FeatureIntegrationId("entry.consumption.eligibility")
internal val ENTRY_CONSUMPTION_MUTATION_RESULT_INTEGRATION =
    FeatureIntegrationId("entry.consumption.mutation-result")
internal val ENTRY_CONSUMPTION_LIFECYCLE_INTEGRATION =
    FeatureIntegrationId("entry.consumption.download-lifecycle")

internal object EntryConsumptionProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.consumption.provider-behavior")
}

internal object EntryConsumptionEligibilityBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.consumption.eligibility-behavior")
}

internal object EntryConsumptionMutationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.consumption.mutation-behavior")
}

internal object EntryConsumptionLifecycleBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.consumption.lifecycle-behavior")
}

private enum class EntryConsumptionProviderBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    TYPE_APPLICABILITY(FeatureArtifactId("entry.consumption.type-applicability")),
    PROVIDER_DISPATCH(FeatureArtifactId("entry.consumption.provider-dispatch")),
}

private enum class EntryConsumptionEligibilityBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    ENTRY_ACTIONS(FeatureArtifactId("entry.consumption.entry-actions")),
    LIBRARY_ACTIONS(FeatureArtifactId("entry.consumption.library-actions")),
    UPDATE_ACTIONS(FeatureArtifactId("entry.consumption.update-actions")),
    NOTIFICATION_ACTION(FeatureArtifactId("entry.consumption.notification-action")),
    TRACKING_SYNC(FeatureArtifactId("entry.consumption.tracking-sync")),
}

private object EntryConsumptionLifecycleBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.consumption.download-lifecycle-event")
}

private object EntryConsumptionMutationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.consumption.mutation")
}

internal val ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.consumption.state-change"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.consumption.changed-children"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.consumption.requested-consumed"),
    ContributionOwner("entry-selection"),
)
private val STATE_NO_CHANGE_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.consumption.state-no-change"),
    listOf(ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT),
)
private val NO_CHANGED_CHILDREN_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.consumption.no-changed-children"),
    listOf(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT),
)
private val LIFECYCLE_REQUIRES_CONSUMED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.consumption.lifecycle-requires-consumed"),
    listOf(ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT),
)

internal object EntryConsumptionFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        val consumption = CapabilityExpression.Provided(EntryConsumptionCapability.definition)
        sink.add(
            FeatureContribution(
                feature = ENTRY_CONSUMPTION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_CONSUMPTION_PROVIDER_INTEGRATION,
                        prerequisites = consumption,
                        behaviorProjections = EntryConsumptionProviderBehavior.entries,
                        behavioralContracts = listOf(EntryConsumptionProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_CONSUMPTION_REFERENCE.requirement),
                        projections = listOf(ENTRY_CONSUMPTION_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_CONSUMPTION_ELIGIBILITY_INTEGRATION,
                        prerequisites = consumption,
                        contextInputs = listOf(ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT)) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(listOf(STATE_NO_CHANGE_BLOCKER))
                            }
                        },
                        contextBlockers = listOf(STATE_NO_CHANGE_BLOCKER),
                        behaviorProjections = EntryConsumptionEligibilityBehavior.entries,
                        behavioralContracts = listOf(EntryConsumptionEligibilityBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_CONSUMPTION_MUTATION_RESULT_INTEGRATION,
                        prerequisites = consumption,
                        contextInputs = listOf(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT)) {
                                FeatureContextDecision.Applicable
                            } else {
                                FeatureContextDecision.Blocked(listOf(NO_CHANGED_CHILDREN_BLOCKER))
                            }
                        },
                        contextBlockers = listOf(NO_CHANGED_CHILDREN_BLOCKER),
                        behaviorProjections = listOf(EntryConsumptionMutationBehavior),
                        behavioralContracts = listOf(EntryConsumptionMutationBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_CONSUMPTION_LIFECYCLE_INTEGRATION,
                        prerequisites = consumption,
                        contextInputs = listOf(
                            ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT,
                            ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(NO_CHANGED_CHILDREN_BLOCKER))
                                !evidence.value(ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(LIFECYCLE_REQUIRES_CONSUMED_BLOCKER))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(NO_CHANGED_CHILDREN_BLOCKER, LIFECYCLE_REQUIRES_CONSUMED_BLOCKER),
                        behaviorProjections = listOf(EntryConsumptionLifecycleBehavior),
                        behavioralContracts = listOf(EntryConsumptionLifecycleBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal fun FeatureGraphEvaluation.consumptionTypes(): Set<EntryType> =
    applicableProviderTypes<EntryConsumptionProcessor>(
        feature = ENTRY_CONSUMPTION_FEATURE_ID,
        integration = ENTRY_CONSUMPTION_PROVIDER_INTEGRATION,
        behaviorProjection = EntryConsumptionProviderBehavior.PROVIDER_DISPATCH.id,
    )

internal fun FeatureGraphEvaluation.requireConsumptionEligibilityContext(type: EntryType, canChange: Boolean) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_CONSUMPTION_FEATURE_ID,
        integration = ENTRY_CONSUMPTION_ELIGIBILITY_INTEGRATION,
        behaviorProjections = EntryConsumptionEligibilityBehavior.entries.map(
            EntryConsumptionEligibilityBehavior::id,
        ),
        evidence = listOf(contextEvidence(ENTRY_CONSUMPTION_STATE_CHANGE_CONTEXT, canChange)),
        applicable = canChange,
    )
}

internal fun FeatureGraphEvaluation.requireConsumptionLifecycleContext(
    type: EntryType,
    changed: Boolean,
    consumed: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_CONSUMPTION_FEATURE_ID,
        integration = ENTRY_CONSUMPTION_LIFECYCLE_INTEGRATION,
        behaviorProjections = listOf(EntryConsumptionLifecycleBehavior.id),
        evidence = listOf(
            contextEvidence(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT, changed),
            contextEvidence(ENTRY_CONSUMPTION_REQUESTED_CONSUMED_CONTEXT, consumed),
        ),
        applicable = changed && consumed,
    )
}

internal fun FeatureGraphEvaluation.requireConsumptionMutationContext(type: EntryType, changed: Boolean) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_CONSUMPTION_FEATURE_ID,
        integration = ENTRY_CONSUMPTION_MUTATION_RESULT_INTEGRATION,
        behaviorProjections = listOf(EntryConsumptionMutationBehavior.id),
        evidence = listOf(contextEvidence(ENTRY_CONSUMPTION_CHANGED_CHILDREN_CONTEXT, changed)),
        applicable = changed,
    )
}
