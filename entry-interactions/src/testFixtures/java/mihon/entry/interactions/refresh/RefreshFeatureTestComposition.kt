package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.mockk
import mihon.feature.graph.ContributionOwner

internal fun refreshFeatureTestComposition(
    type: EntryType = EntryType.BOOK,
    automaticDownload: EntryAutomaticDownloadCoordinator = mockk(relaxed = true),
): EntryInteractionComposition {
    val plugin = object : EntryInteractionPlugin {
        override val type = type
        override val owner = ContributionOwner("test.refresh.${type.name.lowercase()}")
        override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
    }
    return createEntryInteractionComposition(
        plugins = listOf(plugin),
        featureContributors = listOf(
            EntrySourceRefreshFeatureContributor,
            EntryLibraryUpdateRefreshFeatureContributor,
            EntryAutomaticDownloadFeatureContributor,
            EntryAutomaticDownloadRefreshContributor,
        ),
        executionBindings = listOf(
            entryAutomaticDownloadSourceRefreshBinding { automaticDownload },
            entryAutomaticDownloadLibraryUpdateBinding { automaticDownload },
        ),
    )
}
