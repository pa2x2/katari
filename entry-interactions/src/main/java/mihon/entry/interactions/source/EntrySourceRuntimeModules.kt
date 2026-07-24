package mihon.entry.interactions

import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

internal val EntrySourceSettingsFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.source-settings",
    contributor = EntrySourceSettingsFeatureContributor,
) {
    addSingletonFactory<EntrySourceSettingsFeature> {
        DefaultEntrySourceSettingsFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntrySourceSettingsFeature>() }),
    )
}

internal val EntrySourceHomeFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.source-home",
    contributor = EntrySourceHomeFeatureContributor,
) {
    addSingletonFactory<EntrySourceHomeFeature> {
        DefaultEntrySourceHomeFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntrySourceHomeFeature>() }),
    )
}

internal val EntryCoverNetworkFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.cover-network",
    contributor = EntryCoverNetworkFeatureContributor,
    additionalContributors = listOf(EntryCoverHashProfileMoveContributor),
) { context ->
    addSingletonFactory<EntryCoverNetworkFeature> {
        DefaultEntryCoverNetworkFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        executionBindings = listOf(
            mihon.feature.graph.FeatureExecutionParticipantBinding(
                definition = ENTRY_COVER_HASH_PROFILE_MOVE_PARTICIPANT,
                handler = mihon.feature.graph.FeatureExecutionHandler { event ->
                    context.dependencies.profileMoveCoverHashStateHost.move(event.plan.stateRequest())
                },
            ),
        ),
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryCoverNetworkFeature>() }),
    )
}

internal val EntrySourceRefreshFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.source-refresh",
    contributor = EntrySourceRefreshFeatureContributor,
) { context ->
    addSingletonFactory<EntrySourceRefreshFeature> {
        DefaultEntrySourceRefreshFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            executions = get<EntryInteractionComposition>().featureExecutions,
            sourceManager = get(),
            syncEntryWithSource = get(),
            updateLibraryTitles = context.dependencies.sourceRefreshUpdateLibraryTitles,
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntrySourceRefreshFeature>() }),
    )
}

internal val EntryWebViewFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.web-view",
    contributor = EntryWebViewFeatureContributor,
) {
    addSingletonFactory<EntryWebViewFeature> {
        DefaultEntryWebViewFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryWebViewFeature>() }),
    )
}

internal val EntryDeepLinkFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.deep-link",
    contributor = EntryDeepLinkFeatureContributor,
) {
    addSingletonFactory<EntryDeepLinkFeature> {
        DefaultEntryDeepLinkFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
            networkToLocalEntry = get(),
            entryChapterRepository = get(),
            sourceRefresh = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryDeepLinkFeature>() }),
    )
}

internal val EntryTrackerSourceAdapterFeatureRuntimeModule = EntryFeatureRuntimeModule(
    id = "entry.tracker-source-adapter",
    contributor = EntryTrackerSourceAdapterFeatureContributor,
) {
    addSingletonFactory<EntryTrackerSourceAdapterFeature> {
        DefaultEntryTrackerSourceAdapterFeature(
            evaluation = get<EntryInteractionComposition>().featureGraphEvaluation,
            sourceManager = get(),
            settings = get(),
            home = get(),
        )
    }
    EntryFeatureRuntimeArtifacts(
        runtimeBoundaries = listOf(entryFeatureRuntimeBoundary { get<EntryTrackerSourceAdapterFeature>() }),
    )
}
