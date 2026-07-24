package mihon.entry.interactions

import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.featureContextRule

internal fun entryTrackingIntegrations(): List<FeatureIntegration> = listOf(
    FeatureIntegration(
        id = EntryTrackingIntegration.REGISTRY.id,
        prerequisites = CapabilityExpression.Always,
        behaviorProjections = listOf(
            EntryTrackingBehavior.SERVICE_REGISTRY,
            EntryTrackingBehavior.SERVICE_PRESENTATION,
            EntryTrackingBehavior.ACCOUNT_SETTINGS,
            EntryTrackingBehavior.BACKUP_DIAGNOSTICS,
        ),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.REGISTRY),
    ),
    FeatureIntegration(
        id = EntryTrackingIntegration.AVAILABILITY.id,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(ENTRY_TRACKING_REGISTERED_SUPPORT),
        contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
            if (evidence.value(ENTRY_TRACKING_REGISTERED_SUPPORT)) {
                FeatureContextDecision.Applicable
            } else {
                FeatureContextDecision.Blocked(listOf(NO_REGISTERED_TRACKER))
            }
        },
        contextBlockers = listOf(NO_REGISTERED_TRACKER),
        behaviorProjections = listOf(
            EntryTrackingBehavior.ENTRY_ACTION,
            EntryTrackingBehavior.DOCUMENTATION,
        ),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.AVAILABILITY),
        projectionRequirements = listOf(ENTRY_TRACKING_REFERENCE.requirement),
        projections = listOf(ENTRY_TRACKING_REFERENCE.projection),
    ),
    FeatureIntegration(
        id = EntryTrackingIntegration.SESSION.id,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_TRACKING_REGISTERED_SUPPORT,
            ENTRY_TRACKING_AUTHENTICATED_SUPPORT,
            ENTRY_TRACKING_SOURCE_ACCEPTED,
        ),
        contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
            when {
                !evidence.value(ENTRY_TRACKING_REGISTERED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_REGISTERED_TRACKER))
                !evidence.value(ENTRY_TRACKING_AUTHENTICATED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_AUTHENTICATED_TRACKER))
                !evidence.value(ENTRY_TRACKING_SOURCE_ACCEPTED) ->
                    FeatureContextDecision.Blocked(listOf(SOURCE_NOT_ACCEPTED))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(NO_REGISTERED_TRACKER, NO_AUTHENTICATED_TRACKER, SOURCE_NOT_ACCEPTED),
        behaviorProjections = listOf(
            EntryTrackingBehavior.ENTRY_SESSION,
            EntryTrackingBehavior.ENTRY_OPERATIONS,
        ),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.SESSION),
    ),
    FeatureIntegration(
        id = EntryTrackingIntegration.AUTOMATIC_BINDING.id,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_TRACKING_REGISTERED_SUPPORT,
            ENTRY_TRACKING_AUTHENTICATED_SUPPORT,
            ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED,
        ),
        contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
            when {
                !evidence.value(ENTRY_TRACKING_REGISTERED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_REGISTERED_TRACKER))
                !evidence.value(ENTRY_TRACKING_AUTHENTICATED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_AUTHENTICATED_TRACKER))
                !evidence.value(ENTRY_TRACKING_AUTOMATIC_SOURCE_ACCEPTED) ->
                    FeatureContextDecision.Blocked(listOf(NO_AUTOMATIC_TRACKER))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(NO_REGISTERED_TRACKER, NO_AUTHENTICATED_TRACKER, NO_AUTOMATIC_TRACKER),
        behaviorProjections = listOf(EntryTrackingBehavior.AUTOMATIC_BINDING),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.AUTOMATIC_BINDING),
    ),
    FeatureIntegration(
        id = EntryTrackingIntegration.SYNCHRONIZATION.id,
        prerequisites = CapabilityExpression.Always,
        contextInputs = listOf(
            ENTRY_TRACKING_REGISTERED_SUPPORT,
            ENTRY_TRACKING_AUTHENTICATED_SUPPORT,
            ENTRY_TRACKING_AUTHENTICATED_TRACK,
        ),
        contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
            when {
                !evidence.value(ENTRY_TRACKING_REGISTERED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_REGISTERED_TRACKER))
                !evidence.value(ENTRY_TRACKING_AUTHENTICATED_SUPPORT) ->
                    FeatureContextDecision.Blocked(listOf(NO_AUTHENTICATED_TRACKER))
                !evidence.value(ENTRY_TRACKING_AUTHENTICATED_TRACK) ->
                    FeatureContextDecision.Blocked(listOf(NO_AUTHENTICATED_TRACK))
                else -> FeatureContextDecision.Applicable
            }
        },
        contextBlockers = listOf(NO_REGISTERED_TRACKER, NO_AUTHENTICATED_TRACKER, NO_AUTHENTICATED_TRACK),
        behaviorProjections = listOf(EntryTrackingBehavior.PROGRESS_SYNCHRONIZATION),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.SYNCHRONIZATION),
    ),
    FeatureIntegration(
        id = EntryTrackingIntegration.MIGRATION_PREPARATION.id,
        prerequisites = CapabilityExpression.Always,
        behaviorProjections = listOf(EntryTrackingBehavior.MIGRATION_PREPARATION),
        behavioralContracts = listOf(EntryTrackingBehaviorContract.MIGRATION_PREPARATION),
    ),
    authenticatedTypeIntegration(
        integration = EntryTrackingIntegration.LIBRARY,
        behaviorProjections = listOf(
            EntryTrackingBehavior.LIBRARY_FILTER_EVIDENCE,
            EntryTrackingBehavior.LIBRARY_SCORE_EVIDENCE,
        ),
        contract = EntryTrackingBehaviorContract.LIBRARY,
    ),
    authenticatedTypeIntegration(
        integration = EntryTrackingIntegration.STATS,
        behaviorProjections = listOf(EntryTrackingBehavior.STATS_EVIDENCE),
        contract = EntryTrackingBehaviorContract.STATS,
    ),
)

private fun authenticatedTypeIntegration(
    integration: EntryTrackingIntegration,
    behaviorProjections: List<EntryTrackingBehavior>,
    contract: EntryTrackingBehaviorContract,
): FeatureIntegration = FeatureIntegration(
    id = integration.id,
    prerequisites = CapabilityExpression.Always,
    contextInputs = listOf(
        ENTRY_TRACKING_REGISTERED_SUPPORT,
        ENTRY_TRACKING_AUTHENTICATED_SUPPORT,
    ),
    contextRule = featureContextRule(ENTRY_TRACKING_OWNER) { evidence ->
        when {
            !evidence.value(ENTRY_TRACKING_REGISTERED_SUPPORT) ->
                FeatureContextDecision.Blocked(listOf(NO_REGISTERED_TRACKER))
            !evidence.value(ENTRY_TRACKING_AUTHENTICATED_SUPPORT) ->
                FeatureContextDecision.Blocked(listOf(NO_AUTHENTICATED_TRACKER))
            else -> FeatureContextDecision.Applicable
        }
    },
    contextBlockers = listOf(NO_REGISTERED_TRACKER, NO_AUTHENTICATED_TRACKER),
    behaviorProjections = behaviorProjections,
    behavioralContracts = listOf(contract),
)
