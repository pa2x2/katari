package mihon.entry.interactions.library

import mihon.entry.interactions.library.membership.EntryLibraryMembershipCoordinator
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeature
import mihon.entry.interactions.library.membership.EntryLibraryMembershipFeatureContributor
import mihon.entry.interactions.library.membership.consequence.ENTRY_LIBRARY_CUSTOM_COVER_REMOVAL_PARTICIPANT
import mihon.entry.interactions.library.membership.consequence.EntryLibraryCustomCoverContributor
import mihon.entry.interactions.merge.EntryMergeNavigationFeature
import mihon.entry.interactions.merge.EntryMergeSubject
import mihon.entry.interactions.runtime.EntryInteractions
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeArtifacts
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeModule
import mihon.entry.interactions.runtime.production.entryFeatureRuntimeBoundary
import mihon.feature.graph.execution.FeatureExecutionHandler
import mihon.feature.graph.execution.FeatureExecutionParticipantBinding
import mihon.feature.runtime.FeatureRuntimeComposition
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
            executions = get<FeatureRuntimeComposition>().executions,
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
        DefaultEntryLibraryFilterFeature(get<FeatureRuntimeComposition>().evaluation)
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
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryLibraryProgressFeature(
            evaluation = composition.evaluation,
            interaction = get<EntryInteractions>().libraryProgress,
            continueFeature = get(),
            entryProgressRepository = get(),
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
            evaluation = get<FeatureRuntimeComposition>().evaluation,
            sourceRefresh = get(),
            executions = get<FeatureRuntimeComposition>().executions,
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
        val composition = get<FeatureRuntimeComposition>()
        DefaultEntryLibraryUpdateNotificationFeature(
            evaluation = composition.evaluation,
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
