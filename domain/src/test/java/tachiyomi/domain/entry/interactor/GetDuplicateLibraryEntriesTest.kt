package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.DuplicateMatchReason
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.domain.track.model.Track
import tachiyomi.domain.track.repository.TrackRepository

class GetDuplicateLibraryEntriesTest {

    private val entryRepository = FakeEntryRepository()
    private val entryChapterRepository = FakeEntryChapterRepository()
    private val mergedEntryRepository = FakeMergedEntryRepository()
    private val trackRepository = FakeTrackRepository()
    private val duplicatePreferences = DuplicatePreferences(InMemoryPreferenceStore()).apply {
        extendedDuplicateDetectionEnabled.set(true)
    }

    private val getDuplicateLibraryEntries = GetDuplicateLibraryEntries(
        entryRepository = entryRepository,
        mergedEntryRepository = mergedEntryRepository,
        entryChapterRepository = entryChapterRepository,
        trackRepository = trackRepository,
        duplicatePreferences = duplicatePreferences,
    )

    @Test
    fun `returns strong match for normalized same title`() = runTest {
        val description =
            "The mage Frieren journeys onward after the demon king falls and reflects on the lives she outlived."
        val current = entry(
            id = 1,
            title = "Frieren: Beyond Journey's End",
            description = description,
        )
        val duplicate = entry(
            id = 2,
            title = "Frieren Beyond Journeys End",
            author = "Kanehito Yamada",
            description = description,
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 140)

        val results = getDuplicateLibraryEntries(current)

        results shouldHaveSize 1
        results.single().entry.id shouldBe 2L
        results.single().score shouldBeGreaterThanOrEqual 40
        results.single().reasons shouldContain DuplicateMatchReason.TITLE
        results.single().cheapScore shouldBe results.single().score
    }

    @Test
    fun `does not double count exact normalized title weight`() = runTest {
        duplicatePreferences.titleWeight.set(20)
        duplicatePreferences.descriptionWeight.set(30)
        duplicatePreferences.authorWeight.set(0)
        duplicatePreferences.artistWeight.set(0)
        duplicatePreferences.genreWeight.set(0)
        duplicatePreferences.statusWeight.set(0)
        duplicatePreferences.chapterCountWeight.set(0)
        val description =
            "The mage Frieren journeys onward after the demon king falls and reflects on the lives she outlived."
        val current = entry(id = 1, title = "One-Punch Man", description = description)
        val duplicate = entry(
            id = 2,
            title = "One Punch Man",
            author = "Different Author",
            description = description,
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 140)

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.TITLE
        result.score shouldBe 50
        result.score shouldBeLessThan 70
    }

    @Test
    fun `uses creator and status markers to boost likely duplicate`() = runTest {
        duplicatePreferences.descriptionWeight.set(34)
        duplicatePreferences.authorWeight.set(11)
        duplicatePreferences.artistWeight.set(7)
        duplicatePreferences.coverWeight.set(14)
        duplicatePreferences.genreWeight.set(9)
        duplicatePreferences.statusWeight.set(4)
        duplicatePreferences.chapterCountWeight.set(4)
        duplicatePreferences.titleWeight.set(17)
        val description =
            "Noor keeps parrying impossible attacks and accidentally becomes the strongest while believing he is weak."
        val current = entry(
            id = 1,
            title = "I Parry Everything",
            author = "Nabeshiki",
            status = EntryStatus.ONGOING,
            genre = listOf("Action", "Fantasy"),
            description = description,
        )
        val duplicate = entry(
            id = 2,
            title = "I Parry Everything: What Do You Mean I'm the Strongest",
            author = "Nabeshiki",
            status = EntryStatus.ONGOING,
            genre = listOf("Fantasy", "Action"),
            description = description,
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 40)

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.TITLE
        result.reasons shouldContain DuplicateMatchReason.AUTHOR
        result.reasons shouldContain DuplicateMatchReason.STATUS
        result.reasons shouldContain DuplicateMatchReason.GENRE
    }

    @Test
    fun `returns merged library members as aggregate candidate`() = runTest {
        val description = "Japan assembles an elite striker program to create the world's most selfish goal scorer."
        val current = entry(id = 1, title = "Blue Lock Official", description = description)
        val target = entry(id = 10, title = "Blue Lock 1", description = description, favorite = true)
        val member = entry(id = 11, title = "Blue Lock (Official)", description = description, favorite = true)
        entryRepository.libraryEntries.value = listOf(target, member)
        entryChapterRepository.setCounts(10L to 20, 11L to 22)
        mergedEntryRepository.merges.value = listOf(
            EntryMerge(targetId = 10, entryId = 10, position = 0),
            EntryMerge(targetId = 10, entryId = 11, position = 1),
        )

        val results = getDuplicateLibraryEntries(current)

        results.single().entry.id shouldBe 10L
        results.single().count shouldBe 42L
        results.single().reasons shouldContain DuplicateMatchReason.TITLE
    }

    @Test
    fun `configured exclusions are applied on both titles in extended mode`() = runTest {
        duplicatePreferences.titleExclusionPatterns.set(listOf("[*]"))
        val current = entry(
            id = 1,
            title = "One Punch Man [English] [Scanlator]",
            description = LONG_DESCRIPTION,
        )
        val duplicate = entry(
            id = 2,
            title = "One Punch Man [Spanish] [Another Group]",
            description = LONG_DESCRIPTION,
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 140)

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.TITLE
        result.entry.id shouldBe 2L
    }

    @Test
    fun `ignores low similarity titles`() = runTest {
        val current = entry(
            id = 1,
            title = "Frieren",
            description = "An elf mage travels after the end of the hero's journey and learns what life means.",
        )
        val unrelated = entry(
            id = 2,
            title = "One Piece",
            description = "A rubber pirate sails the seas to find a legendary treasure and become king of the pirates.",
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(unrelated)
        entryChapterRepository.setCounts(2L to 1000)

        getDuplicateLibraryEntries(current) shouldBe emptyList()
    }

    @Test
    fun `matches on strong description despite different title`() = runTest {
        duplicatePreferences.descriptionWeight.set(34)
        duplicatePreferences.authorWeight.set(11)
        duplicatePreferences.artistWeight.set(7)
        duplicatePreferences.coverWeight.set(14)
        duplicatePreferences.genreWeight.set(9)
        duplicatePreferences.statusWeight.set(4)
        duplicatePreferences.chapterCountWeight.set(4)
        duplicatePreferences.titleWeight.set(17)
        val description =
            "Encrid dreamed of becoming a knight, but those words poisoned his childhood and he keeps returning to today."
        val current = entry(
            id = 1,
            title = "Eternally Regressing Knight",
            author = "Kanara",
            status = EntryStatus.ONGOING,
            genre = listOf("Action", "Fantasy", "Regression"),
            description = description,
        )
        val duplicate = entry(
            id = 2,
            title = "The Knight Only Lives Today",
            author = "Ian",
            status = EntryStatus.ONGOING,
            genre = listOf("Action", "Fantasy", "Manhwa"),
            description = description,
            favorite = true,
        )
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 105)

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.DESCRIPTION
        result.reasons shouldContain DuplicateMatchReason.STATUS
        result.reasons shouldContain DuplicateMatchReason.GENRE
        result.score shouldBeGreaterThanOrEqual 32
    }

    @Test
    fun `filters weak matches below configured minimum score`() = runTest {
        duplicatePreferences.descriptionWeight.set(0)
        duplicatePreferences.authorWeight.set(0)
        duplicatePreferences.artistWeight.set(0)
        duplicatePreferences.coverWeight.set(0)
        duplicatePreferences.genreWeight.set(0)
        duplicatePreferences.statusWeight.set(0)
        duplicatePreferences.chapterCountWeight.set(0)
        duplicatePreferences.titleWeight.set(40)
        duplicatePreferences.minimumMatchScore.set(45)
        val current = entry(id = 1, title = "One-Punch Man")
        val duplicate = entry(id = 2, title = "One Punch Man", favorite = true)
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 140)

        getDuplicateLibraryEntries(current) shouldBe emptyList()

        duplicatePreferences.minimumMatchScore.set(40)

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.TITLE
        result.score shouldBe 40
    }

    @Test
    fun `keeps tracker-only matches even below content thresholds`() = runTest {
        val current = entry(id = 1, title = "Alpha Series", description = "Short description")
        val duplicate = entry(id = 2, title = "Totally Different Title", favorite = true)
        val trackerId = 7L
        val remoteId = 99L
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 12)
        trackRepository.tracks.value = listOf(
            track(id = 1, entryId = 1, trackerId = trackerId, remoteId = remoteId),
            track(id = 2, entryId = 2, trackerId = trackerId, remoteId = remoteId),
        )

        val result = getDuplicateLibraryEntries(current).single()

        result.reasons shouldContain DuplicateMatchReason.TRACKER
        result.score shouldBe 58
    }

    @Test
    fun `subscribe updates when library entries change`() = runTest {
        val current = entry(id = 1, title = "Alpha Series", description = "Short description")
        val trackedDuplicate = entry(id = 2, title = "Totally Different Title", favorite = true)
        val trackerId = 7L
        val remoteId = 99L
        entryRepository.libraryEntries.value = emptyList()
        trackRepository.tracks.value = listOf(
            track(id = 1, entryId = 1, trackerId = trackerId, remoteId = remoteId),
        )

        val results = getDuplicateLibraryEntries.subscribe(flowOf(current), backgroundScope)
        val emissions = mutableListOf<List<DuplicateEntryCandidate>>()
        val job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            results.take(2).toList(emissions)
        }

        entryRepository.libraryEntries.value = listOf(trackedDuplicate)
        entryChapterRepository.setCounts(2L to 12)
        trackRepository.tracks.value = listOf(
            track(id = 1, entryId = 1, trackerId = trackerId, remoteId = remoteId),
            track(id = 2, entryId = 2, trackerId = trackerId, remoteId = remoteId),
        )

        testScheduler.advanceUntilIdle()
        job.join()

        emissions shouldHaveSize 2
        emissions.first() shouldBe emptyList()
        emissions.last() shouldHaveSize 1
        emissions.last().single().entry.id shouldBe 2L
    }

    @Test
    fun `subscribe updates when title exclusions change`() = runTest {
        duplicatePreferences.descriptionWeight.set(0)
        duplicatePreferences.authorWeight.set(0)
        duplicatePreferences.artistWeight.set(0)
        duplicatePreferences.coverWeight.set(0)
        duplicatePreferences.genreWeight.set(0)
        duplicatePreferences.statusWeight.set(0)
        duplicatePreferences.chapterCountWeight.set(0)
        duplicatePreferences.titleWeight.set(40)
        duplicatePreferences.titleExclusionPatterns.set(emptyList())
        val current = entry(id = 1, title = "One Punch Man [English]")
        val duplicate = entry(id = 2, title = "One Punch Man [Spanish]", favorite = true)
        entryRepository.libraryEntries.value = listOf(duplicate)
        entryChapterRepository.setCounts(2L to 140)

        val results = getDuplicateLibraryEntries.subscribe(flowOf(current), backgroundScope)
        val emissions = mutableListOf<List<DuplicateEntryCandidate>>()
        val job = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            results.take(2).toList(emissions)
        }

        duplicatePreferences.titleExclusionPatterns.set(listOf("[*]"))

        testScheduler.advanceUntilIdle()
        job.join()

        emissions shouldHaveSize 2
        emissions.first() shouldBe emptyList()
        emissions.last().single().entry.id shouldBe 2L
    }

    @Test
    fun `ignores cross-type duplicate candidates`() = runTest {
        duplicatePreferences.descriptionWeight.set(0)
        duplicatePreferences.authorWeight.set(0)
        duplicatePreferences.artistWeight.set(0)
        duplicatePreferences.coverWeight.set(0)
        duplicatePreferences.genreWeight.set(0)
        duplicatePreferences.statusWeight.set(0)
        duplicatePreferences.chapterCountWeight.set(0)
        duplicatePreferences.titleWeight.set(40)
        val current = entry(id = 1, title = "One Punch Man", type = EntryType.MANGA)
        val animeDuplicate = entry(id = 2, title = "One Punch Man", favorite = true, type = EntryType.ANIME)
        val mangaDuplicate = entry(id = 3, title = "One Punch Man", favorite = true, type = EntryType.MANGA)
        entryRepository.libraryEntries.value = listOf(animeDuplicate, mangaDuplicate)
        entryChapterRepository.setCounts(2L to 12, 3L to 140)

        val results = getDuplicateLibraryEntries(current)

        results.map { it.entry.id } shouldBe listOf(3L)
    }

    private fun entry(
        id: Long,
        title: String,
        author: String? = null,
        status: EntryStatus = EntryStatus.UNKNOWN,
        genre: List<String>? = null,
        description: String? = null,
        favorite: Boolean = false,
        type: EntryType = EntryType.MANGA,
    ): Entry {
        return Entry.create().copy(
            id = id,
            source = id,
            favorite = favorite,
            title = title,
            author = author,
            status = status,
            genre = genre,
            description = description,
            initialized = true,
            url = "/entry/$id",
            lastModifiedAt = id,
            type = type,
        )
    }

    private fun chapter(id: Long, entryId: Long): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = entryId,
            url = "/chapter/$id",
        )
    }

    private fun track(
        id: Long,
        entryId: Long,
        trackerId: Long,
        remoteId: Long,
    ): Track {
        return Track(
            id = id,
            entryId = entryId,
            trackerId = trackerId,
            remoteId = remoteId,
            libraryId = null,
            title = "",
            progress = 0.0,
            total = 0,
            status = 0,
            score = 0.0,
            remoteUrl = "",
            startDate = 0,
            finishDate = 0,
            private = false,
        )
    }

    private class FakeEntryRepository : EntryRepository {
        val libraryEntries = MutableStateFlow<List<Entry>>(emptyList())

        override suspend fun getEntryById(id: Long): Entry? = libraryEntries.value.firstOrNull { it.id == id }
        override suspend fun getEntryByIdAsFlow(
            id: Long,
        ): Flow<Entry> = flowOf(libraryEntries.value.first { it.id == id })
        override suspend fun getEntryByUrlAndSourceId(
            url: String,
            sourceId: Long,
            type: EntryType,
        ): Entry? = null
        override suspend fun getEntryByUrlAndSourceId(
            url: String,
            sourceId: Long,
            type: EntryType,
            profileId: Long,
        ): Entry? = null
        override fun getEntryByUrlAndSourceIdAsFlow(
            url: String,
            sourceId: Long,
            type: EntryType,
        ): Flow<Entry?> {
            return flowOf(null)
        }
        override fun getEntryByUrlAndSourceIdAsFlow(
            url: String,
            sourceId: Long,
            type: EntryType,
            profileId: Long,
        ): Flow<Entry?> {
            return flowOf(null)
        }
        override suspend fun getFavorites(): List<Entry> = libraryEntries.value.filter(Entry::favorite)
        override suspend fun getNonFavoriteIds(entryIds: List<Long>): List<Long> = emptyList()
        override suspend fun getFavoritesByProfile(profileId: Long): List<Entry> = getFavorites()
        override suspend fun getAllEntriesByProfile(profileId: Long): List<Entry> = libraryEntries.value
        override suspend fun getReadEntriesNotInLibrary(): List<Entry> = emptyList()
        override suspend fun getReadEntriesNotInLibraryByProfile(profileId: Long): List<Entry> = emptyList()
        override suspend fun getLibraryEntries(): List<Entry> = libraryEntries.value
        override fun getLibraryEntriesAsFlow(): Flow<List<Entry>> = libraryEntries.asStateFlow()
        override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>> {
            return flowOf(libraryEntries.value.filter { it.favorite && it.source == sourceId })
        }
        override suspend fun getUpcomingEntries(
            statuses: Set<Int>,
            types: Set<EntryType>,
        ): Flow<List<Entry>> = flowOf(emptyList())
        override suspend fun resetViewerFlags(): Boolean = true
        override suspend fun setCategories(entryId: Long, categoryIds: List<Long>) = Unit
        override suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean = true
        override suspend fun insert(entry: Entry): Long = entry.id
        override suspend fun insertOrUpdate(entry: Entry): Entry = entry
        override suspend fun update(entry: Entry): Boolean = true
        override suspend fun updateFromSource(entry: Entry): Boolean = true
        override suspend fun setFavorite(id: Long, favorite: Boolean): Boolean = true
        override suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean = true
        override suspend fun setChapterFlags(id: Long, flags: Long): Boolean = true
        override suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean = true
        override suspend fun delete(id: Long): Boolean = true
        override suspend fun deleteNonFavorite(): Boolean = true
        override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? = null
        override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) = Unit
    }

    private class FakeEntryChapterRepository : EntryChapterRepository {
        private val chapters = MutableStateFlow<List<EntryChapter>>(emptyList())

        fun setCounts(vararg counts: Pair<Long, Int>) {
            chapters.value = counts.flatMap { (entryId, count) ->
                (1..count).map { index ->
                    EntryChapter.create().copy(
                        id = entryId * 1000 + index,
                        entryId = entryId,
                        url = "/chapter/${entryId}_$index",
                    )
                }
            }
        }

        override suspend fun getChapterById(id: Long): EntryChapter? = chapters.value.firstOrNull { it.id == id }
        override fun getChaptersByEntryId(entryId: Long): Flow<List<EntryChapter>> {
            return flowOf(chapters.value.filter { it.entryId == entryId })
        }
        override fun getChaptersByEntryIds(entryIds: List<Long>): Flow<List<EntryChapter>> {
            return flowOf(chapters.value.filter { it.entryId in entryIds })
        }
        override suspend fun getChaptersByEntryIdAwait(
            entryId: Long,
            applyScanlatorFilter: Boolean,
        ): List<EntryChapter> {
            return chapters.value.filter { it.entryId == entryId }
        }
        override suspend fun getRecentRead(offset: Int, limit: Int): List<EntryChapter> = emptyList()
        override suspend fun getBookmarkedChaptersByEntryId(entryId: Long): List<EntryChapter> = emptyList()
        override suspend fun insert(chapter: EntryChapter): Long = chapter.id
        override suspend fun insertOrUpdate(chapters: List<EntryChapter>): List<EntryChapter> = chapters
        override suspend fun update(chapter: EntryChapter): Boolean = true
        override suspend fun updateAll(chapters: List<EntryChapter>): Boolean = true
        override suspend fun delete(id: Long): Boolean = true
        override suspend fun deleteByEntryId(entryId: Long): Boolean = true
        override suspend fun removeChaptersWithIds(chapterIds: List<Long>) = Unit
        override suspend fun getScanlatorsByEntryId(entryId: Long): List<String> = emptyList()
        override fun getScanlatorsByEntryIdAsFlow(entryId: Long): Flow<List<String>> = flowOf(emptyList())
        override suspend fun getChapterByUrlAndEntryId(url: String, entryId: Long): EntryChapter? = null
    }

    private class FakeMergedEntryRepository : MergedEntryRepository {
        val merges = MutableStateFlow<List<EntryMerge>>(emptyList())

        override suspend fun getAll(): List<EntryMerge> = merges.value
        override fun subscribeAll(): Flow<List<EntryMerge>> = merges.asStateFlow()
        override suspend fun getGroupByEntryId(entryId: Long): List<EntryMerge> {
            val targetId = merges.value.firstOrNull { it.entryId == entryId }?.targetId ?: return emptyList()
            return merges.value.filter { it.targetId == targetId }
        }
        override fun subscribeGroupByEntryId(entryId: Long): Flow<List<EntryMerge>> = flowOf(emptyList())
        override suspend fun getGroupByTargetId(targetEntryId: Long): List<EntryMerge> {
            return merges.value.filter { it.targetId == targetEntryId }
        }
        override suspend fun getTargetId(
            entryId: Long,
        ): Long? = merges.value.firstOrNull { it.entryId == entryId }?.targetId
        override fun subscribeTargetId(entryId: Long): Flow<Long?> = flowOf(null)
        override suspend fun upsertGroup(targetEntryId: Long, orderedEntryIds: List<Long>) = Unit
        override suspend fun removeMembers(targetEntryId: Long, entryIds: List<Long>) = Unit
        override suspend fun deleteGroup(targetEntryId: Long) = Unit
    }

    private class FakeTrackRepository : TrackRepository {
        val tracks = MutableStateFlow<List<Track>>(emptyList())

        override suspend fun getTrackById(id: Long): Track? = tracks.value.firstOrNull { it.id == id }
        override suspend fun getTracksByEntryId(entryId: Long): List<Track> = tracks.value.filter {
            it.entryId ==
                entryId
        }
        override fun getTracksAsFlow(): Flow<List<Track>> = tracks.asStateFlow()
        override fun getTracksByEntryIdAsFlow(entryId: Long): Flow<List<Track>> = flowOf(
            tracks.value.filter { it.entryId == entryId },
        )
        override suspend fun delete(entryId: Long, trackerId: Long) = Unit
        override suspend fun insert(track: Track) = Unit
        override suspend fun insertAll(tracks: List<Track>) = Unit
    }

    private companion object {
        private const val LONG_DESCRIPTION =
            "Long enough description text to pass normalization and act as stable duplicate evidence for tests."
    }
}
