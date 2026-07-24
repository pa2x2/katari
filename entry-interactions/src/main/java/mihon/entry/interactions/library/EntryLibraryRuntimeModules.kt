package mihon.entry.interactions

import mihon.feature.graph.FeatureExecutionHandler
import mihon.feature.graph.FeatureExecutionParticipantBinding
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.EntryLibraryProgressResolutionPort
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntryLibraryMembershipFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.library-membership",
    contributor = EntryLibraryMembershipFeatureContributor,
    additionalContributors = listOf(EntryLibraryCustomCoverContributor),
) { context ->
    addSingletonFactory<EntryLibraryMembershipFeature> {
        EntryLibraryMembershipCoordinator(
            host = context.dependencies.libraryMembershipHost,
            mergeCandidates = get(),
            executions = get<EntryInteractionComposition>().featureExecutions,
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            FeatureExecutionParticipantBinding(
                definition = ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT,
                handler = FeatureExecutionHandler { event ->
                    event.entries.forEach { entry ->
                        context.dependencies.libraryCustomCoverHost.cleanupAfterLibraryRemoval(entry)
                    }
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryLibraryMembershipFeature>() }),
    )
}

internal val EntryLibraryFilterFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.library-filtering",
    contributor = EntryLibraryFilterFeatureContributor,
) {
    addSingletonFactory<EntryLibraryFilterFeature> {
        DefaultEntryLibraryFilterFeature(get<EntryInteractionComposition>().featureGraphEvaluation)
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryLibraryFilterFeature>() }),
    )
}

internal val EntryLibraryProgressFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.library-progress",
    contributor = EntryLibraryProgressFeatureContributor,
) {
    addSingletonFactory<EntryLibraryProgressFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryLibraryProgressFeature(
            evaluation = composition.featureGraphEvaluation,
            interaction = composition.interactions.libraryProgress,
            continueFeature = get(),
        )
    }
    addSingletonFactory<EntryLibraryProgressResolutionPort> { get<EntryLibraryProgressFeature>() }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryLibraryProgressFeature>() }),
    )
}

internal val EntryLibraryUpdateRefreshFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.library-update-refresh",
    contributor = EntryLibraryUpdateRefreshFeatureContributor,
) {
    addSingletonFactory<EntryLibraryUpdateRefreshFeature> {
        DefaultEntryLibraryUpdateRefreshFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceRefresh = get(),
            executions = get<EntryInteractionComposition>().featureExecutions,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryLibraryUpdateRefreshFeature>() }),
    )
}

internal val EntryLibraryUpdateNotificationFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.library-update-notifications",
    contributor = EntryLibraryUpdateNotificationFeatureContributor,
) {
    addSingletonFactory<EntryLibraryUpdateNotificationFeature> {
        val composition = get<EntryInteractionComposition>()
        DefaultEntryLibraryUpdateNotificationFeature(
            evaluation = composition.featureGraphEvaluation,
            presentationFeature = get(),
            openFeature = get(),
            consumptionFeature = get(),
            downloadActionFeature = get(),
            sourceManager = get(),
            resolveVisibleEntry = { entry ->
                val visibleEntryId = get<EntryMergeNavigationFeature>()
                    .resolveNavigation(EntryMergeSubject(entry.profileId, entry.id))
                    .visibleEntryId
                get<EntryRepository>().getEntryById(visibleEntryId, entry.profileId) ?: entry
            },
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryLibraryUpdateNotificationFeature>() }),
    )
}
