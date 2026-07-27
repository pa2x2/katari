package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.FeatureDurableExecutionParticipantBinding
import mihon.feature.graph.FeatureExecutionParticipantBinding
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeInputs
import mihon.feature.runtime.createFeatureRuntimeComposition
import tachiyomi.domain.entry.model.Entry

fun createEntryInteractions(
    plugins: List<EntryInteractionPlugin>,
    featureContributors: List<FeatureGraphContributor>,
    executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
): EntryInteractions = createEntryInteractionComposition(
    plugins,
    featureContributors,
    executionBindings,
    durableExecutionBindings,
).interactions

data class EntryInteractionInstallation(
    val interactions: EntryInteractions,
    val featureRuntimeInputs: FeatureRuntimeInputs,
)

data class EntryInteractionComposition(
    val interactions: EntryInteractions,
    val featureRuntime: FeatureRuntimeComposition,
) {
    val featureGraph
        get() = featureRuntime.graph

    val featureGraphEvaluation
        get() = featureRuntime.evaluation

    val featureArtifacts
        get() = featureRuntime.artifacts

    val featureExecutions
        get() = featureRuntime.executions
}

fun createEntryInteractionComposition(
    plugins: List<EntryInteractionPlugin>,
    featureContributors: List<FeatureGraphContributor>,
    executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
): EntryInteractionComposition {
    val installation = createEntryInteractionInstallation(
        plugins = plugins,
        featureContributors = featureContributors,
        executionBindings = executionBindings,
        durableExecutionBindings = durableExecutionBindings,
    )
    return EntryInteractionComposition(
        interactions = installation.interactions,
        featureRuntime = createFeatureRuntimeComposition(listOf(installation.featureRuntimeInputs)),
    )
}

fun createEntryInteractionInstallation(
    plugins: List<EntryInteractionPlugin>,
    featureContributors: List<FeatureGraphContributor>,
    executionBindings: List<FeatureExecutionParticipantBinding<*>> = emptyList(),
    durableExecutionBindings: List<FeatureDurableExecutionParticipantBinding<*>> = emptyList(),
): EntryInteractionInstallation {
    validateEntryInteractionPlugins(plugins)
    val providers = EntryInteractionProviderIndex(plugins)
    return EntryInteractionInstallation(
        interactions = DefaultEntryInteractions(
            openProcessors = providers[EntryOpenCapability],
            continueProcessors = providers[EntryContinueCapability],
            downloadProcessors = providers[EntryDownloadCapability],
            downloadOptionsProcessors = providers[EntryDownloadOptionsCapability],
            bulkDownloadCandidateProcessors = providers[EntryBulkDownloadCandidateCapability],
            consumptionProcessors = providers[EntryConsumptionCapability],
            bookmarkProcessors = providers[EntryBookmarkCapability],
            progressProcessors = providers[EntryProgressCapability],
            playbackPreferencesProcessors = providers[EntryPlaybackPreferencesCapability],
            childListProcessors = providers[EntryChildListCapability],
            childProgressProcessors = providers[EntryChildProgressCapability],
            missingChildGapProcessors = providers[EntryMissingChildGapCapability],
            childGroupFilterProcessors = providers[EntryChildGroupFilterCapability],
            previewProcessors = providers[EntryPreviewCapability],
            previewConfigurationProviders = providers[EntryPreviewConfigurationCapability],
            immersiveProcessors = providers[EntryImmersiveCapability],
            libraryProgressProviders = providers[EntryLibraryProgressCapability],
            typePresentationProviders = providers[EntryTypePresentationCapability],
            viewerSettingsProviders = providers[EntryViewerSettingsCapability],
            mediaCacheProviders = providers[EntryMediaCacheCapability],
        ),
        featureRuntimeInputs = FeatureRuntimeInputs(
            graphContributors = plugins + featureContributors,
            executionBindings = executionBindings,
            durableExecutionBindings = durableExecutionBindings,
        ),
    )
}

private fun validateEntryInteractionPlugins(plugins: List<EntryInteractionPlugin>) {
    plugins.forEach(EntryInteractionPlugin::validateContribution)
    plugins.groupBy(EntryInteractionPlugin::type)
        .filterValues { it.size > 1 }
        .forEach { (type, duplicates) ->
            error(
                "Duplicate Entry interaction plugin for $type from " +
                    duplicates.map { it.owner }.distinct().sortedBy { it.value },
            )
        }
}

private class EntryInteractionProviderIndex(
    plugins: List<EntryInteractionPlugin>,
) {
    private val bindings = plugins.flatMap(EntryInteractionPlugin::providerBindings)

    operator fun <P : EntryInteractionProvider> get(
        capability: EntryInteractionCapability<P>,
    ): Map<EntryType, P> {
        return buildMap {
            bindings
                .filter { it.capability === capability }
                .forEach { binding ->
                    @Suppress("UNCHECKED_CAST")
                    val provider = binding.implementation as P
                    val previous = put(provider.type, provider)
                    check(previous == null) {
                        "Duplicate provider for ${capability.definition.id} and EntryType ${provider.type}"
                    }
                }
        }
    }
}

private class DefaultEntryInteractions(
    openProcessors: Map<EntryType, EntryOpenProcessor>,
    continueProcessors: Map<EntryType, EntryContinueProcessor>,
    downloadProcessors: Map<EntryType, EntryDownloadProcessor>,
    downloadOptionsProcessors: Map<EntryType, EntryDownloadOptionsProcessor>,
    bulkDownloadCandidateProcessors: Map<EntryType, EntryBulkDownloadCandidateProcessor>,
    consumptionProcessors: Map<EntryType, EntryConsumptionProcessor>,
    bookmarkProcessors: Map<EntryType, EntryBookmarkProcessor>,
    progressProcessors: Map<EntryType, EntryProgressProcessor>,
    playbackPreferencesProcessors: Map<EntryType, EntryPlaybackPreferencesProcessor>,
    childListProcessors: Map<EntryType, EntryChildListProcessor>,
    childProgressProcessors: Map<EntryType, EntryChildProgressProcessor>,
    missingChildGapProcessors: Map<EntryType, EntryMissingChildGapProcessor>,
    childGroupFilterProcessors: Map<EntryType, EntryChildGroupFilterProcessor>,
    previewProcessors: Map<EntryType, EntryPreviewProcessor>,
    previewConfigurationProviders: Map<EntryType, EntryPreviewConfigurationProvider>,
    immersiveProcessors: Map<EntryType, EntryImmersiveProcessor>,
    libraryProgressProviders: Map<EntryType, EntryLibraryProgressProvider>,
    typePresentationProviders: Map<EntryType, EntryTypePresentationProvider>,
    viewerSettingsProviders: Map<EntryType, EntryViewerSettingsProvider>,
    mediaCacheProviders: Map<EntryType, EntryMediaCacheProvider>,
) : EntryInteractions {
    override val open: EntryOpenInteraction = ProviderBackedEntryOpenInteraction(openProcessors)
    override val continueEntry: EntryContinueInteraction = ProviderBackedEntryContinueInteraction(continueProcessors)
    override val download: EntryDownloadInteraction =
        EntryDownloadInteractionDispatch(
            processors = downloadProcessors,
            optionsProcessors = downloadOptionsProcessors,
            bulkCandidateProcessors = bulkDownloadCandidateProcessors,
        )
    override val consumption: EntryConsumptionInteraction =
        ProviderBackedEntryConsumptionInteraction(consumptionProcessors)
    override val bookmark: EntryBookmarkInteraction = ProviderBackedEntryBookmarkInteraction(bookmarkProcessors)
    override val progress: EntryProgressInteraction = ProviderBackedEntryProgressInteraction(progressProcessors)
    override val playbackPreferences: EntryPlaybackPreferencesInteraction =
        ProviderBackedEntryPlaybackPreferencesInteraction(playbackPreferencesProcessors)
    override val childList: EntryChildListInteraction =
        ProviderBackedEntryChildListInteraction(childListProcessors)
    override val childProgress: EntryChildProgressInteraction =
        ProviderBackedEntryChildProgressInteraction(childProgressProcessors)
    override val missingChildGap: EntryMissingChildGapInteraction =
        ProviderBackedEntryMissingChildGapInteraction(missingChildGapProcessors)
    override val childGroupFilter: EntryChildGroupFilterInteraction =
        ProviderBackedEntryChildGroupFilterInteraction(childGroupFilterProcessors)
    override val preview: EntryPreviewInteraction =
        ProviderBackedEntryPreviewInteraction(previewProcessors, previewConfigurationProviders)
    override val immersive: EntryImmersiveInteraction =
        ProviderBackedEntryImmersiveInteraction(immersiveProcessors)
    override val libraryProgress: EntryLibraryProgressInteraction =
        ProviderBackedEntryLibraryProgressInteraction(libraryProgressProviders)
    override val typePresentation: EntryTypePresentationInteraction =
        ProviderBackedEntryTypePresentationInteraction(typePresentationProviders)
    override val viewerSettings: EntryViewerSettingsInteraction =
        ProviderBackedEntryViewerSettingsInteraction(viewerSettingsProviders)
    override val mediaCache: EntryMediaCacheInteraction =
        ProviderBackedEntryMediaCacheInteraction(mediaCacheProviders)
}
