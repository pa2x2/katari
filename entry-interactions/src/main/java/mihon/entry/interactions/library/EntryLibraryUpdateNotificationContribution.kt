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

internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_ID = FeatureId("entry.library-update-notifications")
private val ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_OWNER = ContributionOwner("entry-library-update-notifications")
private val ENTRY_LIBRARY_UPDATE_NOTIFICATION_REFERENCE = entryContentTypeReferenceContribution(
    id = "library-update-notifications",
    owner = ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.LIBRARY_AND_UPDATES,
    label = "Receive library update notifications",
    order = 300,
)

internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.participation")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.presentation")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.open-participation")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.open-child")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.consumption-participation")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.mark-consumed")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID =
    FeatureIntegrationId("entry.library-update-notifications.download")

internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_ROUTE_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.route")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_RENDER_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.render")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_QUEUE_WARNING_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.queue-warning")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.presentation-vocabulary")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.open-participation")
private val ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_ACTION_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.open-child-action")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.consumption-participation")
private val ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_ACTION_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.mark-consumed-action")
internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_BEHAVIOR_ID =
    FeatureArtifactId("entry.library-update-notifications.download-action")
private val ENTRY_LIBRARY_UPDATE_NOTIFICATION_BEHAVIOR_CONTRACT_ID =
    FeatureArtifactId("entry.library-update-notifications.behavior")

private class NotificationBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection

internal object EntryLibraryUpdateNotificationBehaviorContract : FeatureBehaviorContract {
    override val id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_BEHAVIOR_CONTRACT_ID
}

internal object EntryLibraryUpdateNotificationPresentationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.presentation-behavior")
}

internal object EntryLibraryUpdateNotificationOpenBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.open-behavior")
}

internal object EntryLibraryUpdateNotificationOpenActionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.open-child-behavior")
}

internal object EntryLibraryUpdateNotificationConsumptionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.consumption-behavior")
}

internal object EntryLibraryUpdateNotificationConsumptionActionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.mark-consumed-behavior")
}

internal object EntryLibraryUpdateNotificationDownloadBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-update-notifications.download-behavior")
}

internal val ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.library-update-notifications.has-children"),
    ContributionOwner("entry-selection"),
)
private val OPEN_WITHOUT_CHILDREN_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.library-update-notifications.open-without-children"),
    listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT),
)
private val CONSUMPTION_WITHOUT_CHILDREN_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.library-update-notifications.consume-without-children"),
    listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT),
)

private fun hasChildrenRule(owner: ContributionOwner, blocker: FeatureContextBlocker) =
    featureContextRule(owner) { evidence ->
        if (evidence.value(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT)) {
            FeatureContextDecision.Applicable
        } else {
            FeatureContextDecision.Blocked(listOf(blocker))
        }
    }

internal object EntryLibraryUpdateNotificationFeatureContributor : FeatureGraphContributor {
    override val owner = ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_BASE_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_ROUTE_BEHAVIOR_ID),
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_RENDER_BEHAVIOR_ID),
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_QUEUE_WARNING_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationBehaviorContract),
                        projectionRequirements = listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_REFERENCE.requirement),
                        projections = listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryTypePresentationCapability.definition),
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_PRESENTATION_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationPresentationBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryOpenCapability.definition),
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationOpenBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryOpenCapability.definition),
                        contextInputs = listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT),
                        contextRule = hasChildrenRule(owner, OPEN_WITHOUT_CHILDREN_BLOCKER),
                        contextBlockers = listOf(OPEN_WITHOUT_CHILDREN_BLOCKER),
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_ACTION_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationOpenActionBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryConsumptionCapability.definition),
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationConsumptionBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryConsumptionCapability.definition),
                        contextInputs = listOf(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT),
                        contextRule = hasChildrenRule(owner, CONSUMPTION_WITHOUT_CHILDREN_BLOCKER),
                        contextBlockers = listOf(CONSUMPTION_WITHOUT_CHILDREN_BLOCKER),
                        behaviorProjections = listOf(
                            NotificationBehavior(
                                ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_ACTION_BEHAVIOR_ID,
                            ),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationConsumptionActionBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_INTEGRATION_ID,
                        prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
                        behaviorProjections = listOf(
                            NotificationBehavior(ENTRY_LIBRARY_UPDATE_NOTIFICATION_DOWNLOAD_BEHAVIOR_ID),
                        ),
                        behavioralContracts = listOf(EntryLibraryUpdateNotificationDownloadBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal fun FeatureGraphEvaluation.requireLibraryUpdateNotificationOpenContext(
    type: EntryType,
    hasChildren: Boolean,
) {
    requireLibraryUpdateNotificationActionContext(
        type = type,
        integration = ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_CONTEXT_INTEGRATION_ID,
        behaviorProjection = ENTRY_LIBRARY_UPDATE_NOTIFICATION_OPEN_ACTION_BEHAVIOR_ID,
        hasChildren = hasChildren,
    )
}

internal fun FeatureGraphEvaluation.requireLibraryUpdateNotificationConsumptionContext(
    type: EntryType,
    hasChildren: Boolean,
) {
    requireLibraryUpdateNotificationActionContext(
        type = type,
        integration = ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_CONTEXT_INTEGRATION_ID,
        behaviorProjection = ENTRY_LIBRARY_UPDATE_NOTIFICATION_CONSUMPTION_ACTION_BEHAVIOR_ID,
        hasChildren = hasChildren,
    )
}

private fun FeatureGraphEvaluation.requireLibraryUpdateNotificationActionContext(
    type: EntryType,
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
    hasChildren: Boolean,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_ID,
        integration = integration,
        behaviorProjections = listOf(behaviorProjection),
        evidence = listOf(contextEvidence(ENTRY_LIBRARY_UPDATE_NOTIFICATION_HAS_CHILDREN_CONTEXT, hasChildren)),
        applicable = hasChildren,
    )
}

internal fun FeatureGraphEvaluation.libraryUpdateNotificationTypes(
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
): Set<EntryType> {
    val contentTypes = behaviorProjections.asSequence()
        .filter { applicability ->
            applicability.subject.feature == ENTRY_LIBRARY_UPDATE_NOTIFICATION_FEATURE_ID &&
                applicability.subject.integration == integration &&
                applicability.projection.id == behaviorProjection
        }
        .mapTo(mutableSetOf()) { it.subject.contentType }
    return EntryType.entries.filterTo(mutableSetOf()) { it.toContentTypeId() in contentTypes }
}
