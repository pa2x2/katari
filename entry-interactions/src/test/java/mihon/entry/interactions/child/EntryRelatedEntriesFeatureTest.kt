package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.RelatedEntriesSource
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.entryItemOrientation
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mihon.feature.graph.ContributionOwner
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.NetworkToLocalEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.model.EntrySourceDescription
import tachiyomi.domain.source.service.EntrySourceDescriptionResolutionPort
import tachiyomi.domain.source.service.SourceManager
import java.io.IOException

class EntryRelatedEntriesFeatureTest {

    @Test
    fun `source absence and unsupported source are structured contextual results`() = runTest {
        val origin = entry(ORIGIN_ID, "/origin", EntryType.BOOK)
        val repository = repository(origin)
        val sourceManager = mockk<SourceManager> {
            every { get(SOURCE_ID) } returns null
        }
        val feature = featureFor(sourceManager, repository)

        feature.availability(EntryRelatedEntriesContext(origin, null)) shouldBe
            EntryRelatedEntriesAvailability.Unavailable(EntryRelatedEntriesUnavailableReason.SOURCE_MISSING)
        feature.load(ORIGIN_ID) shouldBe
            EntryRelatedEntriesLoadResult.Unavailable(EntryRelatedEntriesUnavailableReason.SOURCE_MISSING)

        val unsupported = mockk<UnifiedSource> {
            every { id } returns SOURCE_ID
        }
        every { sourceManager.get(SOURCE_ID) } returns unsupported

        feature.availability(EntryRelatedEntriesContext(origin, unsupported)) shouldBe
            EntryRelatedEntriesAvailability.Unavailable(EntryRelatedEntriesUnavailableReason.SOURCE_UNSUPPORTED)
        feature.load(ORIGIN_ID) shouldBe
            EntryRelatedEntriesLoadResult.Unavailable(EntryRelatedEntriesUnavailableReason.SOURCE_UNSUPPORTED)
        coVerify(exactly = 0) { repository.insertOrUpdate(any()) }
    }

    @Test
    fun `load preserves order authoritative mixed types profile identity persistence and orientation`() = runTest {
        val origin = entry(ORIGIN_ID, "/origin", EntryType.BOOK)
        val source = relatedSource(
            listOf(
                sourceEntry("/same", "Manga", EntryType.MANGA),
                sourceEntry("/same", "Duplicate Manga", EntryType.MANGA),
                sourceEntry("/same", "Anime", EntryType.ANIME),
            ),
            EntryItemOrientation.HORIZONTAL,
        )
        var nextId = 20L
        val repository = repository(origin) { networkEntry ->
            networkEntry.copy(
                id = nextId++,
                profileId = PROFILE_ID,
                favorite = networkEntry.type == EntryType.ANIME,
            )
        }
        val feature = featureFor(sourceManager(source), repository)

        feature.availability(EntryRelatedEntriesContext(origin, source)) shouldBe
            EntryRelatedEntriesAvailability.Available(EntryItemOrientation.HORIZONTAL)
        val result = feature.load(ORIGIN_ID) as EntryRelatedEntriesLoadResult.Loaded

        result.orientation shouldBe EntryItemOrientation.HORIZONTAL
        result.entries.map(Entry::type) shouldContainExactly listOf(EntryType.MANGA, EntryType.ANIME)
        result.entries.map(Entry::profileId) shouldContainExactly listOf(PROFILE_ID, PROFILE_ID)
        result.entries.map(Entry::favorite) shouldContainExactly listOf(false, true)
        coVerify(exactly = 2) { repository.insertOrUpdate(any()) }
    }

    @Test
    fun `persisted entry observation keeps library membership live`() = runTest {
        val origin = entry(ORIGIN_ID, "/origin", EntryType.BOOK)
        val initial = entry(20L, "/related", EntryType.ANIME)
        val favorite = initial.copy(favorite = true)
        val repository = repository(origin)
        every {
            repository.getEntryByUrlAndSourceIdAsFlow(initial.url, initial.source, initial.type)
        } returns flowOf(favorite)
        val feature = featureFor(sourceManager(null), repository)

        feature.observeEntry(initial).first() shouldBe favorite
    }

    @Test
    fun `genuine source failure remains retryable operation failure`() = runTest {
        val origin = entry(ORIGIN_ID, "/origin", EntryType.BOOK)
        val source = relatedSource(emptyList(), EntryItemOrientation.VERTICAL)
        coEvery { source.getRelatedEntries(any()) } throws IOException("network failed")
        val feature = featureFor(sourceManager(source), repository(origin))

        shouldThrow<IOException> { feature.load(ORIGIN_ID) }
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
        coEvery { insertOrUpdate(any()) } answers { persist(firstArg()) }
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
