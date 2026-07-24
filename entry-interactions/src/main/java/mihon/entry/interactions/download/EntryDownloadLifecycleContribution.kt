package mihon.entry.interactions

import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.entry.interactions.documentation.entryContentTypeReferenceNoteContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContextInputDefinition
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContextBlocker
import mihon.feature.graph.FeatureContextDecision
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureExecutionOrder
import mihon.feature.graph.FeatureExecutionParticipantDefinition
import mihon.feature.graph.FeatureExecutionParticipantId
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import mihon.feature.graph.contextInputDefinition
import mihon.feature.graph.featureContextRule

internal val ENTRY_DOWNLOAD_LIFECYCLE_FEATURE_ID = FeatureId("entry.download.lifecycle")
private val FEATURE_OWNER = ContributionOwner("entry-download-lifecycle")
private val ENTRY_DOWNLOAD_CONSUMED_CLEANUP_REFERENCE = entryContentTypeReferenceContribution(
    id = "download-consumed-cleanup",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    label = "Delete downloads after marking an item consumed",
    order = 400,
)
private val ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_REFERENCE = entryContentTypeReferenceNoteContribution(
    id = "download-bookmark-cleanup-note",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.DOWNLOADS,
    text = "Bookmark-aware download cleanup is enabled automatically when the content type supports individual " +
        "bookmarks.",
    order = 600,
)
internal val ENTRY_DOWNLOAD_LIFECYCLE_PROVIDER_INTEGRATION = FeatureIntegrationId("entry.download.lifecycle.provider")
internal val ENTRY_DOWNLOAD_MARKED_CONSUMED_INTEGRATION =
    FeatureIntegrationId("entry.download.lifecycle.marked-consumed")
internal val ENTRY_DOWNLOAD_COMPLETION_INTEGRATION = FeatureIntegrationId("entry.download.lifecycle.completion")
internal val ENTRY_DOWNLOAD_AHEAD_INTEGRATION = FeatureIntegrationId("entry.download.lifecycle.download-ahead")
internal val ENTRY_DOWNLOAD_CLEANUP_OWNER_INTEGRATION =
    FeatureIntegrationId("entry.download.lifecycle.cleanup-owner")
internal val ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_PROVIDER_INTEGRATION =
    FeatureIntegrationId("entry.download.lifecycle.bookmark-protection.provider")
internal val ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_CONTEXT_INTEGRATION =
    FeatureIntegrationId("entry.download.lifecycle.bookmark-protection.context")

internal object EntryDownloadLifecycleProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.provider-behavior")
}

internal object EntryDownloadMarkedConsumedBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.marked-consumed-behavior")
}

internal object EntryDownloadCompletionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.completion-behavior")
}

internal object EntryDownloadAheadBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.download-ahead-behavior")
}

internal object EntryDownloadCleanupOwnerBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.cleanup-owner-behavior")
}

internal object EntryDownloadBookmarkProtectionProviderBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.bookmark-protection.provider-behavior")
}

internal object EntryDownloadBookmarkProtectionContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.bookmark-protection.context-behavior")
}

internal object EntryDownloadMediaSessionBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.download.lifecycle.media-session.behavior")
}

internal val ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT = FeatureExecutionParticipantDefinition(
    id = FeatureExecutionParticipantId("entry.download.lifecycle.media-session"),
    owner = FEATURE_OWNER,
    point = ENTRY_MEDIA_SESSION_CONSEQUENCE_EXECUTION_POINT,
    prerequisites = allOf(
        CapabilityExpression.Provided(EntryMediaSessionCapability.definition),
        CapabilityExpression.Provided(EntryDownloadCapability.definition),
    ),
    order = FeatureExecutionOrder(after = setOf(ENTRY_PROGRESS_MEDIA_SESSION_PARTICIPANT.id)),
    behavioralContracts = listOf(EntryDownloadMediaSessionBehaviorContract),
)

internal enum class EntryDownloadLifecycleProviderBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    TYPE_APPLICABILITY(FeatureArtifactId("entry.download.lifecycle.type-applicability")),
    EVENT_ACCEPTANCE(FeatureArtifactId("entry.download.lifecycle.events")),
    PROVIDER_DISPATCH(FeatureArtifactId("entry.download.lifecycle.provider-dispatch")),
}

internal object EntryDownloadLifecycleMarkedConsumedBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.lifecycle.marked-consumed-cleanup")
}

internal object EntryDownloadLifecycleCompletionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.lifecycle.completion-cleanup")
}

internal object EntryDownloadLifecycleDownloadAheadBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.lifecycle.download-ahead")
}

internal enum class EntryDownloadLifecycleCleanupBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    CATEGORY_POLICY(FeatureArtifactId("entry.download.lifecycle.category-exclusions")),
    PHYSICAL_DISPATCH(FeatureArtifactId("entry.download.lifecycle.physical-dispatch")),
}

internal object EntryDownloadLifecycleBookmarkProtectionProviderBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.lifecycle.bookmark-protection.provider")
}

internal object EntryDownloadLifecycleBookmarkProtectionBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.download.lifecycle.bookmark-protection")
}

internal val ENTRY_DOWNLOAD_REMOVE_MARKED_CONSUMED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.remove-marked-consumed"),
    ContributionOwner("entry-download-configuration"),
)
internal val ENTRY_DOWNLOAD_COMPLETION_CLEANUP_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.completion-cleanup-enabled"),
    ContributionOwner("entry-download-configuration"),
)
internal val ENTRY_DOWNLOAD_VIEWER_PROGRESS_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.viewer-progress-eligible"),
    ContributionOwner("entry-viewer-state"),
)
internal val ENTRY_DOWNLOAD_AHEAD_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.download-ahead-enabled"),
    ContributionOwner("entry-download-configuration"),
)
internal val ENTRY_DOWNLOAD_CLEANUP_CATEGORY_ALLOWED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.category-allowed"),
    ContributionOwner("entry-category-policy"),
)
internal val ENTRY_DOWNLOAD_REMOVE_BOOKMARKED_CONTEXT = contextInputDefinition<Boolean>(
    ContextInputId("entry.download.lifecycle.remove-bookmarked"),
    ContributionOwner("entry-download-configuration"),
)

private val MARKED_CONSUMED_DISABLED = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.marked-consumed-disabled"),
    listOf(ENTRY_DOWNLOAD_REMOVE_MARKED_CONSUMED_CONTEXT),
)
private val COMPLETION_DISABLED = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.completion-disabled"),
    listOf(ENTRY_DOWNLOAD_COMPLETION_CLEANUP_CONTEXT),
)
private val VIEWER_PROGRESS_INELIGIBLE = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.viewer-progress-ineligible"),
    listOf(ENTRY_DOWNLOAD_VIEWER_PROGRESS_CONTEXT),
)
private val DOWNLOAD_AHEAD_DISABLED = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.download-ahead-disabled"),
    listOf(ENTRY_DOWNLOAD_AHEAD_CONTEXT),
)
private val CATEGORY_EXCLUDED = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.category-excluded"),
    listOf(ENTRY_DOWNLOAD_CLEANUP_CATEGORY_ALLOWED_CONTEXT),
)
private val BOOKMARK_PROTECTION_OVERRIDDEN = FeatureContextBlocker(
    FeatureArtifactId("entry.download.lifecycle.bookmark-protection-overridden"),
    listOf(ENTRY_DOWNLOAD_REMOVE_BOOKMARKED_CONTEXT),
)

internal object EntryDownloadLifecycleFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        val download = CapabilityExpression.Provided(EntryDownloadCapability.definition)
        val downloadAndBookmark = allOf(
            download,
            CapabilityExpression.Provided(EntryBookmarkCapability.definition),
        )
        sink.add(
            FeatureContribution(
                feature = ENTRY_DOWNLOAD_LIFECYCLE_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_LIFECYCLE_PROVIDER_INTEGRATION,
                        prerequisites = download,
                        behaviorProjections = EntryDownloadLifecycleProviderBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadLifecycleProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_CONSUMED_CLEANUP_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_CONSUMED_CLEANUP_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_MARKED_CONSUMED_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_REMOVE_MARKED_CONSUMED_CONTEXT),
                        contextRule = booleanRule(
                            owner,
                            ENTRY_DOWNLOAD_REMOVE_MARKED_CONSUMED_CONTEXT,
                            MARKED_CONSUMED_DISABLED,
                        ),
                        contextBlockers = listOf(MARKED_CONSUMED_DISABLED),
                        behaviorProjections = listOf(EntryDownloadLifecycleMarkedConsumedBehavior),
                        behavioralContracts = listOf(EntryDownloadMarkedConsumedBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_COMPLETION_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_COMPLETION_CLEANUP_CONTEXT),
                        contextRule = booleanRule(
                            owner,
                            ENTRY_DOWNLOAD_COMPLETION_CLEANUP_CONTEXT,
                            COMPLETION_DISABLED,
                        ),
                        contextBlockers = listOf(COMPLETION_DISABLED),
                        behaviorProjections = listOf(EntryDownloadLifecycleCompletionBehavior),
                        behavioralContracts = listOf(EntryDownloadCompletionBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_AHEAD_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_VIEWER_PROGRESS_CONTEXT, ENTRY_DOWNLOAD_AHEAD_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            when {
                                !evidence.value(ENTRY_DOWNLOAD_VIEWER_PROGRESS_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(VIEWER_PROGRESS_INELIGIBLE))
                                !evidence.value(ENTRY_DOWNLOAD_AHEAD_CONTEXT) ->
                                    FeatureContextDecision.Blocked(listOf(DOWNLOAD_AHEAD_DISABLED))
                                else -> FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(VIEWER_PROGRESS_INELIGIBLE, DOWNLOAD_AHEAD_DISABLED),
                        behaviorProjections = listOf(EntryDownloadLifecycleDownloadAheadBehavior),
                        behavioralContracts = listOf(EntryDownloadAheadBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_CLEANUP_OWNER_INTEGRATION,
                        prerequisites = download,
                        contextInputs = listOf(ENTRY_DOWNLOAD_CLEANUP_CATEGORY_ALLOWED_CONTEXT),
                        contextRule = booleanRule(
                            owner,
                            ENTRY_DOWNLOAD_CLEANUP_CATEGORY_ALLOWED_CONTEXT,
                            CATEGORY_EXCLUDED,
                        ),
                        contextBlockers = listOf(CATEGORY_EXCLUDED),
                        behaviorProjections = EntryDownloadLifecycleCleanupBehavior.entries,
                        behavioralContracts = listOf(EntryDownloadCleanupOwnerBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_PROVIDER_INTEGRATION,
                        prerequisites = downloadAndBookmark,
                        behaviorProjections = listOf(EntryDownloadLifecycleBookmarkProtectionProviderBehavior),
                        behavioralContracts = listOf(EntryDownloadBookmarkProtectionProviderBehaviorContract),
                        projectionRequirements = listOf(ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_REFERENCE.requirement),
                        projections = listOf(ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_DOWNLOAD_BOOKMARK_PROTECTION_CONTEXT_INTEGRATION,
                        prerequisites = downloadAndBookmark,
                        contextInputs = listOf(ENTRY_DOWNLOAD_REMOVE_BOOKMARKED_CONTEXT),
                        contextRule = featureContextRule(owner) { evidence ->
                            if (evidence.value(ENTRY_DOWNLOAD_REMOVE_BOOKMARKED_CONTEXT)) {
                                FeatureContextDecision.Blocked(listOf(BOOKMARK_PROTECTION_OVERRIDDEN))
                            } else {
                                FeatureContextDecision.Applicable
                            }
                        },
                        contextBlockers = listOf(BOOKMARK_PROTECTION_OVERRIDDEN),
                        behaviorProjections = listOf(EntryDownloadLifecycleBookmarkProtectionBehavior),
                        behavioralContracts = listOf(EntryDownloadBookmarkProtectionContextBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal object EntryDownloadMediaSessionContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(ENTRY_DOWNLOAD_MEDIA_SESSION_PARTICIPANT)
    }
}

private fun booleanRule(
    owner: ContributionOwner,
    input: ContextInputDefinition<Boolean>,
    blocker: FeatureContextBlocker,
) = featureContextRule(owner) { evidence ->
    if (evidence.value(input)) FeatureContextDecision.Applicable else FeatureContextDecision.Blocked(listOf(blocker))
}
