package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.entryContentTypeReferenceNoteContribution
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
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_DOWNLOAD_ACTION_FEATURE_ID = FeatureId("entry.download.actions")
private val FEATURE_OWNER = ContributionOwner("entry-download-actions")
private val ENTRY_DOWNLOAD_INDIVIDUAL_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-individual",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Download individual child items for offline use",
    order = 100,
)
private val ENTRY_DOWNLOAD_BULK_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-bulk",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Bulk-download child items",
    order = 200,
)
private val ENTRY_DOWNLOAD_BOOKMARKED_BULK_REFERENCE = entryContentTypeReferenceNoteContribution(
    id = "download-bookmarked-bulk-note",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    text = "Bookmark-based bulk downloads are enabled automatically when the content type supports individual " +
        "bookmarks.",
    order = 500,
)

internal val ENTRY_DOWNLOAD_INDIVIDUAL_PROVIDER_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.individual.provider")
internal val ENTRY_DOWNLOAD_BULK_PROVIDER_INTEGRATION = FeatureIntegrationId("entry.download.actions.bulk.provider")
internal val ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.bulk.bookmarked.provider")
internal val ENTRY_DOWNLOAD_INDIVIDUAL_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.individual.context")
internal val ENTRY_DOWNLOAD_INDIVIDUAL_OPERATION_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.individual.operation")
internal val ENTRY_DOWNLOAD_BULK_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.download.actions.bulk.context")
internal val ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.bulk.bookmarked.context")
internal val ENTRY_DOWNLOAD_NOTIFICATION_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.download.actions.notification.context")

internal object EntryDownloadIndividualProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.individual.provider-behavior")
}

internal object EntryDownloadBulkProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.bulk.provider-behavior")
}

internal object EntryDownloadBookmarkedBulkProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.bulk.bookmarked.provider-behavior")
}

internal object EntryDownloadIndividualContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.individual.context-behavior")
}

internal object EntryDownloadIndividualOperationBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.individual.operation-behavior")
}

internal object EntryDownloadBulkContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.bulk.context-behavior")
}

internal object EntryDownloadBookmarkedBulkContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.bulk.bookmarked.context-behavior")
}

internal object EntryDownloadNotificationContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.actions.notification.context-behavior")
}

internal object EntryDownloadIndividualProviderBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.actions.individual.provider-dispatch")
}

internal object EntryDownloadBulkProviderBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.actions.bulk.provider-dispatch")
}

internal object EntryDownloadBookmarkedBulkProviderBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.actions.bulk.bookmarked.provider-dispatch")
}

internal enum class EntryDownloadIndividualBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.download.actions.individual.availability")),
    CANCEL(FeatureArtifactId("entry.download.actions.individual.cancel")),
    RETRY(FeatureArtifactId("entry.download.actions.individual.retry")),
}

internal enum class EntryDownloadIndividualOperationBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    DOWNLOAD(FeatureArtifactId("entry.download.actions.individual.download")),
    DELETE(FeatureArtifactId("entry.download.actions.individual.delete")),
}

internal enum class EntryDownloadBulkBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.download.actions.bulk.availability")),
    RESOLUTION(FeatureArtifactId("entry.download.actions.bulk.resolve")),
}

internal enum class EntryDownloadBookmarkedBulkBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    AVAILABILITY(FeatureArtifactId("entry.download.actions.bulk.bookmarked.availability")),
    RESOLUTION(FeatureArtifactId("entry.download.actions.bulk.bookmarked.resolve")),
}

internal object EntryDownloadNotificationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.actions.notification.action")
}

internal enum class EntryDownloadSelectionState {
    ACTIONABLE,
    EMPTY,
    NOTIFICATION_LIMIT_EXCEEDED,
}

internal val ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT = entrySourceContextInputDefinition<EntryDownloadSourceAccess>(
    id = ContextInputId("entry.download.actions.source-access"),
    nonContractReason = "Remote, Local, and stub access is application source state, not a public SDK contract",
)
internal val ENTRY_DOWNLOAD_SELECTION_CONTEXT = contextInputDefinition<EntryDownloadSelectionState>(
    ContextInputId("entry.download.actions.selection"),
    ContributionOwner("entry-selection"),
)
private val LOCAL_OR_STUB_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.actions.local-or-stub"),
    listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT),
)
private val EMPTY_SELECTION_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.actions.empty-selection"),
    listOf(ENTRY_DOWNLOAD_SELECTION_CONTEXT),
)
private val NOTIFICATION_SELECTION_TOO_LARGE_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.actions.notification-selection-too-large"),
    listOf(ENTRY_DOWNLOAD_SELECTION_CONTEXT),
)

private fun sourceAccessRule(owner: ContributionOwner) = featureContextRule(owner) { evidence ->
    if (evidence.value(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT) == EntryDownloadSourceAccess.REMOTE) {
        FeatureContextDecision.Applicable
    } else {
        FeatureContextDecision.Blocked(listOf(LOCAL_OR_STUB_BLOCKER))
    }
}

private fun sourceAndNonEmptySelectionRule(owner: ContributionOwner) = featureContextRule(owner) { evidence ->
    when {
        evidence.value(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT) != EntryDownloadSourceAccess.REMOTE ->
            FeatureContextDecision.Blocked(listOf(LOCAL_OR_STUB_BLOCKER))
        evidence.value(ENTRY_DOWNLOAD_SELECTION_CONTEXT) == EntryDownloadSelectionState.EMPTY ->
            FeatureContextDecision.Blocked(listOf(EMPTY_SELECTION_BLOCKER))
        else -> FeatureContextDecision.Applicable
    }
}

private fun sourceAndNotificationSelectionRule(owner: ContributionOwner) = featureContextRule(owner) { evidence ->
    when {
        evidence.value(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT) != EntryDownloadSourceAccess.REMOTE ->
            FeatureContextDecision.Blocked(listOf(LOCAL_OR_STUB_BLOCKER))
        evidence.value(ENTRY_DOWNLOAD_SELECTION_CONTEXT) == EntryDownloadSelectionState.EMPTY ->
            FeatureContextDecision.Blocked(listOf(EMPTY_SELECTION_BLOCKER))
        evidence.value(ENTRY_DOWNLOAD_SELECTION_CONTEXT) == EntryDownloadSelectionState.NOTIFICATION_LIMIT_EXCEEDED ->
            FeatureContextDecision.Blocked(listOf(NOTIFICATION_SELECTION_TOO_LARGE_BLOCKER))
        else -> FeatureContextDecision.Applicable
    }
}

internal object EntryDownloadActionFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        val download = CapabilityExpression.Provided(EntryDownloadCapability.definition)
        val bulk = allOf(
            download,
            CapabilityExpression.Provided(EntryBulkDownloadCandidateCapability.definition),
        )
        val bookmarkedBulk = allOf(
            download,
            CapabilityExpression.Provided(EntryBulkDownloadCandidateCapability.definition),
            CapabilityExpression.Provided(EntryBookmarkCapability.definition),
        )
        sink.add(
            FeatureContribution(
                feature = ENTRY_DOWNLOAD_ACTION_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_INDIVIDUAL_PROVIDER_INTEGRATION,
                        prerequisites = download,
                        behaviorProjections = listOf(EntryDownloadIndividualProviderBehavior),
                        behavioralContracts = listOf(EntryDownloadIndividualProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_INDIVIDUAL_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_INDIVIDUAL_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BULK_PROVIDER_INTEGRATION,
                        prerequisites = bulk,
                        behaviorProjections = listOf(EntryDownloadBulkProviderBehavior),
                        behavioralContracts = listOf(EntryDownloadBulkProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_BULK_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_BULK_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BOOKMARKED_BULK_PROVIDER_INTEGRATION,
                        prerequisites = bookmarkedBulk,
                        behaviorProjections = listOf(EntryDownloadBookmarkedBulkProviderBehavior),
                        behavioralContracts = listOf(EntryDownloadBookmarkedBulkProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_BOOKMARKED_BULK_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_BOOKMARKED_BULK_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_INDIVIDUAL_CONTEXT_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT),
                        contextRule = sourceAccessRule(owner),
                        contextBlockers = listOf(LOCAL_OR_STUB_BLOCKER),
                        behaviorProjections = EntryDownloadIndividualBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadIndividualContextBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_INDIVIDUAL_OPERATION_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT, ENTRY_DOWNLOAD_SELECTION_CONTEXT),
                        contextRule = sourceAndNonEmptySelectionRule(owner),
                        contextBlockers = listOf(LOCAL_OR_STUB_BLOCKER, EMPTY_SELECTION_BLOCKER),
                        behaviorProjections = EntryDownloadIndividualOperationBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadIndividualOperationBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BULK_CONTEXT_INTEGRATION,
                        prerequisites = bulk,
                        contextInputs = listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT),
                        contextRule = sourceAccessRule(owner),
                        contextBlockers = listOf(LOCAL_OR_STUB_BLOCKER),
                        behaviorProjections = EntryDownloadBulkBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadBulkContextBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BOOKMARKED_BULK_CONTEXT_INTEGRATION,
                        prerequisites = bookmarkedBulk,
                        contextInputs = listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT),
                        contextRule = sourceAccessRule(owner),
                        contextBlockers = listOf(LOCAL_OR_STUB_BLOCKER),
                        behaviorProjections = EntryDownloadBookmarkedBulkBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadBookmarkedBulkContextBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_NOTIFICATION_CONTEXT_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_SOURCE_ACCESS_CONTEXT, ENTRY_DOWNLOAD_SELECTION_CONTEXT),
                        contextRule = sourceAndNotificationSelectionRule(owner),
                        contextBlockers = listOf(
                            LOCAL_OR_STUB_BLOCKER,
                            EMPTY_SELECTION_BLOCKER,
                            NOTIFICATION_SELECTION_TOO_LARGE_BLOCKER,
                        ),
                        behaviorProjections = listOf(EntryDownloadNotificationBehavior),
                        behavioralContracts = listOf(EntryDownloadNotificationContextBehaviorContract),
                    ),
                ),
            ),
        )
    }
}
