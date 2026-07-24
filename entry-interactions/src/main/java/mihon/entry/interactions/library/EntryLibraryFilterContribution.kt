package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.documentation.EntryContentTypeReferenceSection
import mihon.entry.interactions.documentation.entryContentTypeReferenceContribution
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContentTypeId
import mihon.feature.graph.ContextInputId
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
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

internal val ENTRY_LIBRARY_FILTER_FEATURE_ID = FeatureId("entry.library-filtering")
private val FEATURE_OWNER = ContributionOwner("entry-library-filtering")
private val ENTRY_LIBRARY_FILTER_REFERENCE = entryContentTypeReferenceContribution(
    id = "library-filtering",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.LIBRARY_AND_UPDATES,
    label = "Filter and group entries in the unified library",
    order = 100,
)
internal val ENTRY_LIBRARY_FILTER_PARTICIPATION_INTEGRATION =
    FeatureIntegrationId("entry.library-filtering.participation")
internal val ENTRY_LIBRARY_FILTER_CONTEXT_INTEGRATION = FeatureIntegrationId("entry.library-filtering.context")
internal val ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION =
    FeatureIntegrationId("entry.library-filtering.bookmark-control")
internal val ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION =
    FeatureIntegrationId("entry.library-filtering.progress-controls")
internal val ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION =
    FeatureIntegrationId("entry.library-filtering.outside-release-period")

private object EntryLibraryFilterParticipationBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-filtering.type-participation")
}

private enum class EntryLibraryFilterContextBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    POLICY(FeatureArtifactId("entry.library-filtering.policy")),
    MATCHING(FeatureArtifactId("entry.library-filtering.matching")),
    ACTIVE_STATE(FeatureArtifactId("entry.library-filtering.active-state")),
}

private object EntryLibraryFilterBookmarkControlBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-filtering.bookmark-control")
}

private object EntryLibraryFilterProgressControlBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-filtering.progress-controls")
}

private object EntryLibraryFilterReleasePeriodBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-filtering.outside-release-period")
}

internal object EntryLibraryFilterBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-filtering.behavior")
}

internal object EntryLibraryFilterContextBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-filtering.context-behavior")
}

internal object EntryLibraryFilterProgressBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-filtering.progress-behavior")
}

internal object EntryLibraryFilterBookmarkBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-filtering.bookmark-behavior")
}

internal object EntryLibraryFilterReleasePeriodBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-filtering.outside-release-period-behavior")
}

internal data class EntryLibraryFilterStateContext(
    val targetCount: Int,
    val hasUnknownProgress: Boolean,
    val hasUnknownBookmarks: Boolean,
    val hasDownloadedOrLocal: Boolean,
    val hasOutsideReleasePeriod: Boolean,
)

internal data class EntryLibraryTrackingFilterContext(
    val configuredTrackerIds: Set<Long>,
    val targetTrackerIds: Set<Long>,
)

internal val ENTRY_LIBRARY_FILTER_POLICY_CONTEXT = contextInputDefinition<EntryLibraryFilterPolicy>(
    ContextInputId("entry.library-filtering.preferences"),
    ContributionOwner("entry-library-filter-configuration"),
)
internal val ENTRY_LIBRARY_FILTER_STATE_CONTEXT = contextInputDefinition<EntryLibraryFilterStateContext>(
    ContextInputId("entry.library-filtering.library-state"),
    ContributionOwner("entry-library-state"),
)
internal val ENTRY_LIBRARY_FILTER_TRACKING_CONTEXT = contextInputDefinition<EntryLibraryTrackingFilterContext>(
    ContextInputId("entry.library-filtering.tracking"),
    ContributionOwner("entry-tracking-state"),
)

internal object EntryLibraryFilterFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_LIBRARY_FILTER_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_FILTER_PARTICIPATION_INTEGRATION,
                        prerequisites = CapabilityExpression.Always,
                        behaviorProjections = listOf(EntryLibraryFilterParticipationBehavior),
                        behavioralContracts = listOf(EntryLibraryFilterBehaviorContract),
                        projectionRequirements = listOf(ENTRY_LIBRARY_FILTER_REFERENCE.requirement),
                        projections = listOf(ENTRY_LIBRARY_FILTER_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_FILTER_CONTEXT_INTEGRATION,
                        prerequisites = CapabilityExpression.Always,
                        contextInputs = listOf(
                            ENTRY_LIBRARY_FILTER_POLICY_CONTEXT,
                            ENTRY_LIBRARY_FILTER_STATE_CONTEXT,
                            ENTRY_LIBRARY_FILTER_TRACKING_CONTEXT,
                        ),
                        contextRule = featureContextRule(owner) { FeatureContextDecision.Applicable },
                        behaviorProjections = EntryLibraryFilterContextBehavior.entries,
                        behavioralContracts = listOf(EntryLibraryFilterContextBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION,
                        prerequisites = CapabilityExpression.Provided(EntryLibraryProgressCapability.definition),
                        behaviorProjections = listOf(EntryLibraryFilterProgressControlBehavior),
                        behavioralContracts = listOf(EntryLibraryFilterProgressBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryLibraryProgressCapability.definition),
                            CapabilityExpression.Provided(EntryBookmarkCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryLibraryFilterBookmarkControlBehavior),
                        behavioralContracts = listOf(EntryLibraryFilterBookmarkBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION,
                        prerequisites = CapabilityExpression.Provided(
                            EntryOutsideReleasePeriodFilterCapability.definition,
                        ),
                        behaviorProjections = listOf(EntryLibraryFilterReleasePeriodBehavior),
                        behavioralContracts = listOf(EntryLibraryFilterReleasePeriodBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal data class EntryLibraryFilterGraphSelection(
    val participatingContentTypes: Set<ContentTypeId>,
    val bookmarkTypes: Set<EntryType>,
    val progressTypes: Set<EntryType>,
    val releasePeriodTypes: Set<EntryType>,
)

internal fun FeatureGraphEvaluation.libraryFilterSelection(): EntryLibraryFilterGraphSelection {
    return EntryLibraryFilterGraphSelection(
        participatingContentTypes = selectedContentTypes(
            ENTRY_LIBRARY_FILTER_PARTICIPATION_INTEGRATION,
            EntryLibraryFilterParticipationBehavior.id,
        ),
        bookmarkTypes = applicableProviderTypes<EntryBookmarkProcessor>(
            feature = ENTRY_LIBRARY_FILTER_FEATURE_ID,
            integration = ENTRY_LIBRARY_FILTER_BOOKMARK_INTEGRATION,
            behaviorProjection = EntryLibraryFilterBookmarkControlBehavior.id,
        ),
        progressTypes = applicableProviderTypes<EntryLibraryProgressProvider>(
            feature = ENTRY_LIBRARY_FILTER_FEATURE_ID,
            integration = ENTRY_LIBRARY_FILTER_PROGRESS_INTEGRATION,
            behaviorProjection = EntryLibraryFilterProgressControlBehavior.id,
        ),
        releasePeriodTypes = applicableProviderTypes<EntryOutsideReleasePeriodFilterProvider>(
            feature = ENTRY_LIBRARY_FILTER_FEATURE_ID,
            integration = ENTRY_LIBRARY_FILTER_RELEASE_PERIOD_INTEGRATION,
            behaviorProjection = EntryLibraryFilterReleasePeriodBehavior.id,
        ),
    )
}

internal fun FeatureGraphEvaluation.requireLibraryFilterContext(
    types: Set<EntryType>,
    policy: EntryLibraryFilterPolicy,
    state: EntryLibraryFilterStateContext,
    tracking: EntryLibraryTrackingFilterContext,
) {
    types.forEach { type ->
        requireEntryContextState(
            type = type,
            feature = ENTRY_LIBRARY_FILTER_FEATURE_ID,
            integration = ENTRY_LIBRARY_FILTER_CONTEXT_INTEGRATION,
            behaviorProjections = EntryLibraryFilterContextBehavior.entries.map(
                EntryLibraryFilterContextBehavior::id,
            ),
            evidence = listOf(
                contextEvidence(ENTRY_LIBRARY_FILTER_POLICY_CONTEXT, policy),
                contextEvidence(ENTRY_LIBRARY_FILTER_STATE_CONTEXT, state),
                contextEvidence(ENTRY_LIBRARY_FILTER_TRACKING_CONTEXT, tracking),
            ),
            applicable = true,
        )
    }
}

private fun FeatureGraphEvaluation.selectedContentTypes(
    integration: FeatureIntegrationId,
    behaviorProjection: FeatureArtifactId,
): Set<ContentTypeId> {
    return behaviorProjections
        .asSequence()
        .filter { applicability ->
            applicability.subject.feature == ENTRY_LIBRARY_FILTER_FEATURE_ID &&
                applicability.subject.integration == integration &&
                applicability.projection.id == behaviorProjection
        }
        .mapTo(mutableSetOf()) { it.subject.contentType }
}
