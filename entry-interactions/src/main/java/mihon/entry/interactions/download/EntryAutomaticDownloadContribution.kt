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
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.contextEvidence
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID = FeatureId("entry.download.automatic")
private val FEATURE_OWNER = ContributionOwner("entry-automatic-download")
private val ENTRY_AUTOMATIC_DOWNLOAD_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-automatic",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Automatically download newly discovered child items",
    order = 300,
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_PROVIDER_INTEGRATION =
    FeatureIntegrationId("entry.download.automatic.provider")
internal val ENTRY_AUTOMATIC_DOWNLOAD_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.download.automatic.context")

internal object EntryAutomaticDownloadProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.automatic.provider-behavior")
}

internal object EntryAutomaticDownloadContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.automatic.context-behavior")
}

internal object EntryAutomaticDownloadSourceRefreshBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.automatic.source-refresh.behavior")
}

internal object EntryAutomaticDownloadLibraryUpdateBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.automatic.library-update.behavior")
}

internal val ENTRY_AUTOMATIC_DOWNLOAD_SOURCE_REFRESH_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.automatic.source-refresh"),
    owner = FEATURE_OWNER,
    point = ENTRY_SOURCE_REFRESH_NEW_CHILDREN_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryAutomaticDownloadSourceRefreshBehaviorContract),
)

internal val ENTRY_AUTOMATIC_DOWNLOAD_LIBRARY_UPDATE_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.automatic.library-update"),
    owner = FEATURE_OWNER,
    point = ENTRY_LIBRARY_UPDATE_NEW_CHILDREN_EXECUTION_POINT,
    prerequisites = CapabilityExpression.Provided(EntryDownloadCapability.definition),
    behavioralContracts = listOf(EntryAutomaticDownloadLibraryUpdateBehaviorContract),
)

private object EntryAutomaticDownloadProviderBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.automatic.provider-dispatch")
}

private enum class EntryAutomaticDownloadBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    POLICY(FeatureArtifactId("entry.download.automatic.policy")),
    LIBRARY_UPDATE(FeatureArtifactId("entry.download.automatic.library-update")),
    ENTRY_REFRESH(FeatureArtifactId("entry.download.automatic.entry-refresh")),
}

internal val ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.new-children"),
    ContributionOwner("entry-selection"),
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.enabled"),
    ContributionOwner("entry-download-configuration"),
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.library-membership"),
    ContributionOwner("entry-state"),
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.category-policy"),
    ContributionOwner("entry-category-policy"),
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.unread-only"),
    ContributionOwner("entry-download-configuration"),
)
internal val ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.automatic.eligible-candidates"),
    ContributionOwner("entry-selection"),
)

private val EMPTY_SELECTION_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.automatic.empty-selection"),
    listOf(ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT),
)
private val DISABLED_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.automatic.disabled"),
    listOf(ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT),
)
private val NOT_IN_LIBRARY_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.automatic.not-in-library"),
    listOf(ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT),
)
private val CATEGORY_POLICY_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.automatic.category-policy-rejected"),
    listOf(ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT),
)
private val NO_UNREAD_CANDIDATES_BLOCKER = FeatureContextBlocker(
    FeatureArtifactId("entry.download.automatic.no-unread-candidates"),
    listOf(ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT, ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT),
)

internal object EntryAutomaticDownloadFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        val download = CapabilityExpression.Provided(EntryDownloadCapability.definition)
        sink.add(
            FeatureContribution(
                feature = ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_AUTOMATIC_DOWNLOAD_PROVIDER_INTEGRATION,
                        prerequisites = download,
                        behaviorProjections = listOf(EntryAutomaticDownloadProviderBehavior),
                        behavioralContracts = listOf(EntryAutomaticDownloadProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_AUTOMATIC_DOWNLOAD_REFERENCE.requirement),
                        projections = listOf(ENTRY_AUTOMATIC_DOWNLOAD_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_AUTOMATIC_DOWNLOAD_CONTEXT_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(
                            ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT,
                            ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT,
                            ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT,
                            ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT,
                            ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT,
                            ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(EMPTY_SELECTION_BLOCKER))
                                !evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(DISABLED_BLOCKER))
                                !evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(NOT_IN_LIBRARY_BLOCKER))
                                !evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(CATEGORY_POLICY_BLOCKER))
                                evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT) &&
                                    !evidence.value(ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(NO_UNREAD_CANDIDATES_BLOCKER))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(
                            EMPTY_SELECTION_BLOCKER,
                            DISABLED_BLOCKER,
                            NOT_IN_LIBRARY_BLOCKER,
                            CATEGORY_POLICY_BLOCKER,
                            NO_UNREAD_CANDIDATES_BLOCKER,
                        ),
                        behaviorProjections = EntryAutomaticDownloadBehavior.entries,
                        behavioralContracts = listOf(EntryAutomaticDownloadContextBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal object EntryAutomaticDownloadRefreshContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_AUTOMATIC_DOWNLOAD_SOURCE_REFRESH_PARTICIPANT)
        sink.add(ENTRY_AUTOMATIC_DOWNLOAD_LIBRARY_UPDATE_PARTICIPANT)
    }
}

internal fun FeatureGraphEvaluation.automaticDownloadTypes(): Set<EntryType> =
    applicableProviderTypes<EntryDownloadProcessor>(
        feature = ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID,
        integration = ENTRY_AUTOMATIC_DOWNLOAD_PROVIDER_INTEGRATION,
        behaviorProjection = EntryAutomaticDownloadProviderBehavior.id,
    )

internal fun FeatureGraphEvaluation.requireAutomaticDownloadContext(
    type: EntryType,
    decision: EntryAutomaticDownloadPolicyDecision,
) {
    requireEntryContextState(
        type = type,
        feature = ENTRY_AUTOMATIC_DOWNLOAD_FEATURE_ID,
        integration = ENTRY_AUTOMATIC_DOWNLOAD_CONTEXT_INTEGRATION,
        behaviorProjections = EntryAutomaticDownloadBehavior.entries.map(EntryAutomaticDownloadBehavior::id),
        evidence = listOf(
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_NEW_CHILDREN_CONTEXT, decision.hasNewChapters),
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_ENABLED_CONTEXT, decision.enabled),
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_FAVORITE_CONTEXT, decision.favorite),
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_CATEGORY_ALLOWED_CONTEXT, decision.categoryAllowed),
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_UNREAD_ONLY_CONTEXT, decision.unreadOnly),
            contextEvidence(ENTRY_AUTOMATIC_DOWNLOAD_CANDIDATES_CONTEXT, decision.candidates.isNotEmpty()),
        ),
        applicable = decision.blocker == null,
    )
}
