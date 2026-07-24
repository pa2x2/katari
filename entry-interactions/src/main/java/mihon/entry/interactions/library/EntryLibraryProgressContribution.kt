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
import mihon.feature.graph.allOf

internal val ENTRY_LIBRARY_PROGRESS_FEATURE_ID = FeatureId("entry.library-progress")
private val FEATURE_OWNER = ContributionOwner("entry-library-progress")
private val ENTRY_LIBRARY_PROGRESS_REFERENCE = entryContentTypeReferenceContribution(
    id = "library-progress",
    owner = FEATURE_OWNER,
    section = EntryContentTypeReferenceSection.LIBRARY_AND_UPDATES,
    label = "Aggregate progress across the library",
    order = 200,
)
internal val ENTRY_LIBRARY_PROGRESS_PROVIDER_INTEGRATION = FeatureIntegrationId("entry.library-progress.provider")
internal val ENTRY_LIBRARY_PROGRESS_CONTINUE_INTEGRATION = FeatureIntegrationId("entry.library-progress.continue")
internal val ENTRY_LIBRARY_PROGRESS_BOOKMARK_INTEGRATION = FeatureIntegrationId("entry.library-progress.bookmark")

private enum class EntryLibraryProgressBehavior(
    override val id: FeatureArtifactId,
) : FeatureBehaviorProjection {
    LOAD(FeatureArtifactId("entry.library-progress.load")),
    MERGE(FeatureArtifactId("entry.library-progress.merge")),
    BADGES(FeatureArtifactId("entry.library-progress.badges")),
    SORT_INPUTS(FeatureArtifactId("entry.library-progress.sort-inputs")),
    FILTER_INPUTS(FeatureArtifactId("entry.library-progress.filter-inputs")),
    STATS_INPUTS(FeatureArtifactId("entry.library-progress.stats-inputs")),
    UPDATE_INPUTS(FeatureArtifactId("entry.library-progress.update-inputs")),
}

private object EntryLibraryProgressContinueBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-progress.continue-target")
}

private object EntryLibraryProgressBookmarkBehavior : FeatureBehaviorProjection {
    override val id = FeatureArtifactId("entry.library-progress.bookmarks")
}

internal object EntryLibraryProgressBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-progress.behavior")
}

internal object EntryLibraryProgressContinueBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-progress.continue-behavior")
}

internal object EntryLibraryProgressBookmarkBehaviorContract : FeatureBehaviorContract {
    override val id = FeatureArtifactId("entry.library-progress.bookmark-behavior")
}

internal object EntryLibraryProgressFeatureContributor : FeatureGraphContributor {
    override val owner = FEATURE_OWNER

    override fun contributeTo(sink: FeatureGraphContributionSink) {
        sink.add(
            FeatureContribution(
                feature = ENTRY_LIBRARY_PROGRESS_FEATURE_ID,
                owner = owner,
                integrations = listOf(
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_PROGRESS_PROVIDER_INTEGRATION,
                        prerequisites = CapabilityExpression.Provided(EntryLibraryProgressCapability.definition),
                        behaviorProjections = EntryLibraryProgressBehavior.entries,
                        behavioralContracts = listOf(EntryLibraryProgressBehaviorContract),
                        projectionRequirements = listOf(ENTRY_LIBRARY_PROGRESS_REFERENCE.requirement),
                        projections = listOf(ENTRY_LIBRARY_PROGRESS_REFERENCE.projection),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_PROGRESS_CONTINUE_INTEGRATION,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryLibraryProgressCapability.definition),
                            CapabilityExpression.Provided(EntryContinueCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryLibraryProgressContinueBehavior),
                        behavioralContracts = listOf(EntryLibraryProgressContinueBehaviorContract),
                    ),
                    FeatureIntegration(
                        id = ENTRY_LIBRARY_PROGRESS_BOOKMARK_INTEGRATION,
                        prerequisites = allOf(
                            CapabilityExpression.Provided(EntryLibraryProgressCapability.definition),
                            CapabilityExpression.Provided(EntryBookmarkCapability.definition),
                        ),
                        behaviorProjections = listOf(EntryLibraryProgressBookmarkBehavior),
                        behavioralContracts = listOf(EntryLibraryProgressBookmarkBehaviorContract),
                    ),
                ),
            ),
        )
    }
}

internal data class EntryLibraryProgressGraphSelection(
    val applicableTypes: Set<EntryType>,
    val continueTypes: Set<EntryType>,
    val bookmarkTypes: Set<EntryType>,
)

internal fun FeatureGraphEvaluation.libraryProgressSelection(): EntryLibraryProgressGraphSelection {
    val baseProviderTypes = EntryLibraryProgressBehavior.entries
        .mapTo(mutableSetOf()) { behavior ->
            applicableProviderTypes<EntryLibraryProgressProvider>(
                feature = ENTRY_LIBRARY_PROGRESS_FEATURE_ID,
                integration = ENTRY_LIBRARY_PROGRESS_PROVIDER_INTEGRATION,
                behaviorProjection = behavior.id,
            )
        }
        .singleOrNull()
        ?: error("Library progress behaviors selected different provider sets")

    return EntryLibraryProgressGraphSelection(
        applicableTypes = baseProviderTypes,
        continueTypes = applicableProviderTypes<EntryLibraryProgressProvider>(
            feature = ENTRY_LIBRARY_PROGRESS_FEATURE_ID,
            integration = ENTRY_LIBRARY_PROGRESS_CONTINUE_INTEGRATION,
            behaviorProjection = EntryLibraryProgressContinueBehavior.id,
        ),
        bookmarkTypes = applicableProviderTypes<EntryLibraryProgressProvider>(
            feature = ENTRY_LIBRARY_PROGRESS_FEATURE_ID,
            integration = ENTRY_LIBRARY_PROGRESS_BOOKMARK_INTEGRATION,
            behaviorProjection = EntryLibraryProgressBookmarkBehavior.id,
        ),
    )
}
