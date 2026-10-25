package tachiyomi.domain.entry.interactor

import eu.kanade.tachiyomi.source.entry.ChapterListPart
import eu.kanade.tachiyomi.source.entry.ChapterListPartsSource
import eu.kanade.tachiyomi.source.entry.ChapterListRest
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryPageResult
import eu.kanade.tachiyomi.source.entry.EntryRefresh
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import eu.kanade.tachiyomi.source.entry.SEntry
import eu.kanade.tachiyomi.source.entry.SEntryChapter
import eu.kanade.tachiyomi.source.entry.SEntryChapterGroup
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.domain.entry.model.ChapterListWalk
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryChapterGroup
import tachiyomi.domain.entry.repository.EntryChapterGroupRepository
import tachiyomi.domain.entry.repository.EntryChapterListWalkRepository
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.nextupdate.FetchInterval
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager

/** A chapter list that arrives in parts, newest first, saved part by part against the stored chapters. */
class ChapterListPartsSyncTest {

    @Test
    fun `a newest part leaves the stored chapters it doesn't list in place`() = runTest {
        val store = Store(storedChapters(numbers = 1..100))
        val source = PartsSource(newest = sourceChapters(103 downTo 91), rest = ChapterListRest.More("page-2"))

        store.sync(source)(entry(), fetchDetails = false)

        store.chapterNumbersInOrder() shouldBe (103 downTo 1).toList()
        store.chapter(90).sourceOrder shouldBe storedChapters(numbers = 1..100).single { it.url == "/90" }.sourceOrder
        store.walks.get(1L)!!.token shouldBe "page-2"
    }

    @Test
    fun `an unchanged update keeps older chapters and removes none`() = runTest {
        val store = Store(storedChapters(numbers = 1..100) + storedChapter(number = 7, url = "/gone"))
        val source = PartsSource(newest = sourceChapters(102 downTo 96), rest = ChapterListRest.Unchanged)

        val result = store.sync(source)(entry(), fetchDetails = false)

        result.removedChapterIds.shouldBeEmpty()
        result.insertedChapters.map { it.chapterNumber.toInt() } shouldBe listOf(102, 101)
        store.chapterNumbersInOrder().take(4) shouldBe listOf(102, 101, 100, 99)
        store.chapters.any { it.url == "/gone" } shouldBe true
        store.walks.get(1L) shouldBe null
    }

    @Test
    fun `older parts of a first library load stay out of Updates`() = runTest {
        val store = Store(emptyList())
        val source = PartsSource(
            newest = sourceChapters(30 downTo 21),
            rest = ChapterListRest.More("page-2"),
            older = mapOf("page-2" to ChapterListPart(sourceChapters(20 downTo 1), ChapterListRest.Complete)),
        )
        val libraryEntry = entry().copy(favorite = true, dateAdded = 500L)

        store.sync(source, now = 1_000L)(libraryEntry, fetchDetails = false)
        val joinedLibrary = store.entryUpdates.last().dateAdded
        store.syncOlder(source)(libraryEntry.copy(dateAdded = joinedLibrary))

        joinedLibrary shouldBe 1_000L
        store.chapters.filter { it.chapterNumber <= 20 }.all { it.dateFetch <= joinedLibrary } shouldBe true
        store.chapterNumbersInOrder() shouldBe (30 downTo 1).toList()
    }

    @Test
    fun `a finished walk removes only chapters no part listed`() = runTest {
        val store = Store(storedChapters(numbers = 1..30) + storedChapter(number = 15, url = "/15-old-upload"))
        val source = PartsSource(
            newest = sourceChapters(31 downTo 21),
            rest = ChapterListRest.More("page-2"),
            older = mapOf(
                "page-2" to ChapterListPart(sourceChapters(20 downTo 11), ChapterListRest.More("page-3")),
                "page-3" to ChapterListPart(sourceChapters(10 downTo 1), ChapterListRest.Complete),
            ),
        )

        store.sync(source)(entry(), fetchDetails = false)
        val second = store.syncOlder(source)(entry())
        val third = store.syncOlder(source)(entry())

        second shouldBe SyncOlderChapters.Result.More(insertedChapters = 0, loaded = null, total = null)
        third shouldBe SyncOlderChapters.Result.Finished(removedChapters = 1)
        store.chapters.map { it.url }.sorted() shouldBe (1..31).map { "/$it" }.sorted()
        store.walks.get(1L) shouldBe null
    }

    @Test
    fun `groups declared across parts keep their reading order`() = runTest {
        val store = Store(emptyList())
        fun grouped(range: IntProgression, group: (Int) -> String) = sourceChapters(range).onEach {
            it.group = group(it.chapterNumber.toInt())
        }
        val volume = { number: Int -> "v${(number - 1) / 10 + 1}" }
        val source = PartsSource(
            newest = grouped(30 downTo 26, volume),
            groups = listOf(group("v3")),
            rest = ChapterListRest.More("page-2"),
            older = mapOf(
                // Volume 3 spans this part and the newest one
                "page-2" to ChapterListPart(
                    grouped(25 downTo 16, volume),
                    ChapterListRest.More("page-3"),
                    listOf(group("v2"), group("v3")),
                ),
                "page-3" to ChapterListPart(
                    grouped(15 downTo 1, volume),
                    ChapterListRest.Complete,
                    listOf(group("v1"), group("v2")),
                ),
            ),
        )

        store.sync(source)(entry(), fetchDetails = false)
        store.syncOlder(source)(entry())
        store.syncOlder(source)(entry())

        val groups = store.groups.getByEntryId(1L).sortedBy(EntryChapterGroup::position)
        groups.map { it.key } shouldBe listOf("v1", "v2", "v3")
        val positions = groups.associate { it.key to it.position }
        store.chapters.all { it.groupKey == volume(it.chapterNumber.toInt()) } shouldBe true
        store.chapters.all { it.groupPosition == positions[it.groupKey] } shouldBe true
    }

    private fun entry(): Entry = Entry.create().copy(id = 1L, source = 1L, title = "Entry", type = EntryType.BOOK)

    // Newest first, like a source lists them: chapter 100 has source order 0
    private fun storedChapters(numbers: IntRange): List<EntryChapter> = numbers.reversed().mapIndexed { order, number ->
        storedChapter(number = number, url = "/$number", sourceOrder = order.toLong(), id = number.toLong())
    }

    private fun storedChapter(number: Int, url: String, sourceOrder: Long = 500L, id: Long = 900L + number) =
        EntryChapter.create().copy(
            id = id,
            entryId = 1L,
            url = url,
            name = "Chapter $number",
            chapterNumber = number.toDouble(),
            sourceOrder = sourceOrder,
            dateUpload = 1L,
        )

    private fun sourceChapters(numbers: IntProgression): List<SEntryChapter> = numbers.map { number ->
        SEntryChapter.create().apply {
            url = "/$number"
            name = "Chapter $number"
            chapterNumber = number.toDouble()
            dateUpload = 1L
        }
    }

    private fun group(key: String): SEntryChapterGroup = SEntryChapterGroup.create(key, key.uppercase())

    private class Store(initial: List<EntryChapter>) {
        var chapters = initial
        val groups = InMemoryChapterGroups()
        val walks = InMemoryChapterListWalks()
        val entryUpdates = mutableListOf<Entry>()
        private var nextId = 10_000L

        fun chapter(number: Int): EntryChapter = chapters.single { it.url == "/$number" }

        fun chapterNumbersInOrder(): List<Int> = chapters.sortedBy(EntryChapter::sourceOrder)
            .map { it.chapterNumber.toInt() }

        // Applies writes the way the real repository does, in order, giving inserted chapters ids
        private val chapterRepository: EntryChapterRepository = mockk(relaxed = true) {
            coEvery { getChaptersByEntryIdAwait(1L, any()) } answers { chapters }
            coEvery { applySourceSync(any(), any(), any(), any(), any()) } coAnswers {
                val removed = firstArg<List<Long>>().toSet()
                val updated = secondArg<List<EntryChapter>>().associateBy(EntryChapter::id)
                chapters = chapters.filterNot { it.id in removed }.map { updated[it.id] ?: it }
                arg<suspend () -> Unit>(3).invoke()
                val inserted = thirdArg<List<EntryChapter>>().map { it.copy(id = nextId++) }
                chapters = chapters + inserted
                arg<suspend (List<EntryChapter>) -> Unit>(4).invoke(inserted)
                inserted
            }
        }

        private val entryRepository: EntryRepository = mockk(relaxed = true) {
            coEvery { update(any()) } answers {
                entryUpdates += firstArg<Entry>()
                true
            }
        }

        fun sync(source: PartsSource, now: Long = 1_000L) = SyncEntryWithSource(
            entryRepository = entryRepository,
            entryChapterRepository = chapterRepository,
            entryChapterGroupRepository = groups,
            entryProgressRepository = mockk(relaxed = true),
            sourceManager = sourceManager(source),
            libraryPreferences = LibraryPreferences(InMemoryPreferenceStore()),
            fetchInterval = mockk<FetchInterval> {
                coEvery { update(any(), any(), any(), any(), any()) } answers { firstArg() }
            },
            metadataChangeNotifier = mockk(relaxed = true),
            entryChapterListWalkRepository = walks,
            now = { now },
        )

        fun syncOlder(source: PartsSource) = SyncOlderChapters(
            entryChapterRepository = chapterRepository,
            entryChapterGroupRepository = groups,
            entryProgressRepository = mockk(relaxed = true),
            entryChapterListWalkRepository = walks,
            sourceManager = sourceManager(source),
        )

        private fun sourceManager(source: PartsSource): SourceManager = mockk { every { get(1L) } returns source }
    }

    private class PartsSource(
        private val newest: List<SEntryChapter>,
        private val rest: ChapterListRest,
        private val groups: List<SEntryChapterGroup> = emptyList(),
        private val older: Map<String, ChapterListPart> = emptyMap(),
    ) : ChapterListPartsSource {
        override val id: Long = 1L
        override val name: String = "Source"

        override suspend fun getPopularContent(page: Int): EntryPageResult<SEntry> = error("Not used")
        override suspend fun getLatestUpdates(page: Int): EntryPageResult<SEntry> = error("Not used")
        override suspend fun getSearchContent(
            page: Int,
            query: String,
            filters: EntryFilterList,
        ): EntryPageResult<SEntry> = error("Not used")

        override suspend fun refresh(
            entry: SEntry,
            existingChapters: List<SEntryChapter>,
            fetchDetails: Boolean,
            fetchChapters: Boolean,
        ): EntryRefresh = EntryRefresh(entry, newest, groups, rest)

        override suspend fun getOlderChapters(entry: SEntry, token: String): ChapterListPart = older.getValue(token)

        override suspend fun getMedia(chapter: SEntryChapter, selection: PlaybackSelection): EntryMedia =
            error("Not used")
    }
}

internal class InMemoryChapterGroups : EntryChapterGroupRepository {
    private var stored = emptyList<EntryChapterGroup>()

    override fun subscribe(entryIds: List<Long>): Flow<List<EntryChapterGroup>> =
        flowOf(stored.filter { it.entryId in entryIds })

    override suspend fun getByEntryId(entryId: Long): List<EntryChapterGroup> = stored.filter { it.entryId == entryId }

    override suspend fun replace(entryId: Long, groups: List<EntryChapterGroup>) {
        stored = stored.filterNot { it.entryId == entryId } + groups
    }
}

internal class InMemoryChapterListWalks : EntryChapterListWalkRepository {
    private val walks = mutableMapOf<Long, ChapterListWalk>()
    private val listed = mutableMapOf<Long, MutableSet<Long>>()

    override suspend fun get(entryId: Long): ChapterListWalk? = walks[entryId]

    override suspend fun start(walk: ChapterListWalk) {
        walks[walk.entryId] = walk
        listed[walk.entryId] = mutableSetOf()
    }

    override suspend fun advance(walk: ChapterListWalk) {
        walks[walk.entryId] = walk
    }

    override suspend fun finish(entryId: Long) {
        walks -= entryId
        listed -= entryId
    }

    override suspend fun markListed(entryId: Long, chapterIds: Collection<Long>) {
        if (entryId in walks) listed.getOrPut(entryId) { mutableSetOf() } += chapterIds
    }

    override suspend fun getListedChapterIds(entryId: Long): Set<Long> = listed[entryId].orEmpty()
}
