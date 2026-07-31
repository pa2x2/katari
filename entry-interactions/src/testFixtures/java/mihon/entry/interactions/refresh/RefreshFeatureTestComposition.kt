package mihon.entry.interactions.refresh

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.mockk
import mihon.entry.interactions.download.EntryAutomaticDownloadCoordinator
import mihon.entry.interactions.download.EntryAutomaticDownloadFeatureContributor
import mihon.entry.interactions.download.EntryAutomaticDownloadRefreshContributor
import mihon.entry.interactions.download.entryAutomaticDownloadLibraryUpdateBinding
import mihon.entry.interactions.download.entryAutomaticDownloadSourceRefreshBinding
import mihon.entry.interactions.library.EntryLibraryUpdateRefreshFeatureContributor
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.entry.interactions.source.EntrySourceRefreshFeatureContributor
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
