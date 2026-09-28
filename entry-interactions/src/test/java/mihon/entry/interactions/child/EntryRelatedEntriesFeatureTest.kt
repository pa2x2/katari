package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.entry.interactions.runtime.EntryInteractionComposition
import mihon.entry.interactions.runtime.EntryInteractionPlugin
import mihon.entry.interactions.runtime.EntryInteractionProviderBinding
import mihon.entry.interactions.runtime.createEntryInteractionComposition
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager

class EntryRelatedEntriesFeatureTest {

    @Test
    fun `load persists distinct mixed-type results into the origin entry's profile`() = runTest {
        val origin = entry(ORIGIN_ID, "/origin", EntryType.BOOK)
        val source = relatedSource(
            listOf(
                sourceEntry("/same", "Manga", EntryType.MANGA),
                sourceEntry("/same", "Duplicate Manga", EntryType.MANGA),
                sourceEntry("/same", "Anime", EntryType.ANIME),
            ),
            EntryItemOrientation.HORIZONTAL,
        )
        val persistedNetworkEntries = mutableListOf<Entry>()
        val repository = repository(origin) { networkEntry ->
            persistedNetworkEntries += networkEntry
            networkEntry
        }
        val feature = featureFor(sourceManager(source), repository)

        feature.load(ORIGIN_ID).shouldBeInstanceOf<EntryRelatedEntriesLoadResult.Loaded>()

        persistedNetworkEntries.map(Entry::title) shouldContainExactly listOf("Manga", "Anime")
        coVerify(exactly = 1) { repository.insertOrUpdateBatch(any(), PROFILE_ID) }
    }

    private fun featureFor(
        sourceManager: SourceManager,
        repository: EntryRepository,
    ): EntryRelatedEntriesFeature {
        val composition = compositionFor(EntryType.BOOK)
        return DefaultEntryRelatedEntriesFeature(
            evaluation = composition.featureGraphEvaluation,
            sourceManager = sourceManager,
            networkToLocalEntry = NetworkToLocalEntry(repository),
            getEntry = GetEntry(repository),
            sourceDescription = EntrySourceDescriptionResolutionPort { source ->
                EntrySourceDescription(
                    language = "",
                    supportedEntryTypes = null,
                    itemOrientation = source.entryItemOrientation(),
                    catalogue = null,
                )
            },
        )
    }

    private fun compositionFor(type: EntryType): EntryInteractionComposition {
        val plugin = object : EntryInteractionPlugin {
            override val type = type
            override val owner = ContributionOwner("test.type.${type.name.lowercase()}")
            override val providerBindings = emptyList<EntryInteractionProviderBinding<*>>()
        }
        return createEntryInteractionComposition(
            plugins = listOf(plugin),
            featureContributors = listOf(EntryRelatedEntriesFeatureContributor),
        )
    }

    private fun repository(
        origin: Entry,
        persist: (Entry) -> Entry = { it },
    ): EntryRepository = mockk {
        coEvery { getEntryById(ORIGIN_ID) } returns origin
        coEvery { insertOrUpdateBatch(any(), PROFILE_ID) } answers {
            firstArg<List<Entry>>().map(persist)
        }
    }

    private fun sourceManager(source: UnifiedSource?): SourceManager = mockk {
        every { get(SOURCE_ID) } returns source
    }

    private fun relatedSource(
        entries: List<SEntry>,
        orientation: EntryItemOrientation,
    ): RelatedEntriesSource {
        val source = mockk<RelatedEntriesSource>(
            moreInterfaces = arrayOf(EntryItemOrientationProvider::class),
        ) {
            every { id } returns SOURCE_ID
            coEvery { getRelatedEntries(any()) } returns entries
        }
        every { (source as EntryItemOrientationProvider).itemOrientation } returns orientation
        return source
    }

    private fun entry(id: Long, url: String, type: EntryType): Entry = Entry.create().copy(
        id = id,
        profileId = PROFILE_ID,
        source = SOURCE_ID,
        url = url,
        title = url,
        type = type,
    )

    private fun sourceEntry(url: String, title: String, type: EntryType): SEntry = SEntry.create().apply {
        this.url = url
        this.title = title
        this.type = type
    }

    private companion object {
        const val ORIGIN_ID = 7L
        const val SOURCE_ID = 9L
        const val PROFILE_ID = 3L
    }
}
