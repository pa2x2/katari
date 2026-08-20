package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import logcat.LogPriority
import tachiyomi.core.common.util.lang.toLong
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.data.query.chunkedForSqlQuery
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.EntrySourceSyncRepository
import tachiyomi.domain.entry.repository.LibraryLastReadObserver
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryRepository, EntrySourceSyncRepository, LibraryLastReadObserver {

    override suspend fun getEntryById(id: Long): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryById(id, profileProvider.activeProfileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getEntryById(id: Long, profileId: Long): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryById(id, profileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getEntriesByIds(entryIds: List<Long>): List<Entry> {
        val sortedEntryIds = entryIds.distinct().sorted()
        if (sortedEntryIds.isEmpty()) return emptyList()
        val profileId = profileProvider.activeProfileId
        return handler.await(inTransaction = true) {
            sortedEntryIds.chunkedForSqlQuery().flatMap { entryIdChunk ->
                entriesQueries.getEntriesByIds(profileId, entryIdChunk, EntryMapper::mapEntry).awaitAsList()
            }
        }
    }

    override suspend fun getEntryByIdAsFlow(id: Long): Flow<Entry> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull {
                entriesQueries.getEntryById(id, profileId, EntryMapper::mapEntry)
            }.filterNotNull()
        }
    }

    override suspend fun getEntriesByIdsAsFlow(entryIds: List<Long>): Flow<List<Entry>> {
        val sortedEntryIds = entryIds.distinct().sorted()
        if (sortedEntryIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull {
                entriesQueries.getEntryById(sortedEntryIds.first(), profileId, EntryMapper::mapEntry)
            }.map { Unit }
                .mapLatest {
                    handler.await(inTransaction = true) {
                        sortedEntryIds.chunkedForSqlQuery().flatMap { entryIdChunk ->
                            entriesQueries.getEntriesByIds(profileId, entryIdChunk, EntryMapper::mapEntry)
                                .awaitAsList()
                        }
                    }
                }
                .distinctUntilChanged()
        }
    }

    override suspend fun getEntryByUrlAndSourceId(
        url: String,
        sourceId: Long,
        type: EntryType,
    ): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryByUrlAndSource(
                profileProvider.activeProfileId,
                url,
                sourceId,
                type.name.lowercase(),
                EntryMapper::mapEntry,
            )
        }
    }

    override suspend fun getEntryByUrlAndSourceId(
        url: String,
        sourceId: Long,
        type: EntryType,
        profileId: Long,
    ): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryByUrlAndSource(
                profileId,
                url,
                sourceId,
                type.name.lowercase(),
                EntryMapper::mapEntry,
            )
        }
    }

    override fun getEntryByUrlAndSourceIdAsFlow(
        url: String,
        sourceId: Long,
        type: EntryType,
    ): Flow<Entry?> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull {
                entriesQueries.getEntryByUrlAndSource(
                    profileId,
                    url,
                    sourceId,
                    type.name.lowercase(),
                    EntryMapper::mapEntry,
                )
            }
        }
    }

    override fun getEntryByUrlAndSourceIdAsFlow(
        url: String,
        sourceId: Long,
        type: EntryType,
        profileId: Long,
    ): Flow<Entry?> {
        return handler.subscribeToOneOrNull {
            entriesQueries.getEntryByUrlAndSource(
                profileId,
                url,
                sourceId,
                type.name.lowercase(),
                EntryMapper::mapEntry,
            )
        }
    }

    override suspend fun getFavorites(): List<Entry> {
        return handler.awaitList {
            entriesQueries.getFavorites(profileProvider.activeProfileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getNonFavoriteIds(entryIds: List<Long>): List<Long> {
        if (entryIds.isEmpty()) return emptyList()
        return handler.await {
            entriesQueries.getNonFavoriteIds(profileProvider.activeProfileId, entryIds).awaitAsList()
        }
    }

    override suspend fun getFavoritesByProfile(profileId: Long): List<Entry> {
        return handler.awaitList {
            entriesQueries.getFavorites(profileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getAllEntriesByProfile(profileId: Long): List<Entry> {
        return handler.awaitList {
            entriesQueries.getAllEntries(profileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getReadEntriesNotInLibrary(): List<Entry> {
        return handler.awaitList {
            entriesQueries.getReadEntriesNotInLibrary(profileProvider.activeProfileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getReadEntriesNotInLibraryByProfile(profileId: Long): List<Entry> {
        return handler.awaitList {
            entriesQueries.getReadEntriesNotInLibrary(profileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getNonLibraryEntriesBySources(
        sourceIds: List<Long>,
        keepReadEntries: Boolean,
    ): List<Entry> {
        if (sourceIds.isEmpty()) return emptyList()
        return handler.awaitList {
            entriesQueries.getNonLibraryEntriesBySources(
                profileId = profileProvider.activeProfileId,
                sourceIds = sourceIds,
                keepReadEntries = keepReadEntries.toLong(),
                mapper = EntryMapper::mapEntry,
            )
        }
    }

    override suspend fun getLibraryEntries(): List<Entry> {
        return handler.awaitList {
            entriesQueries.getFavorites(profileProvider.activeProfileId, EntryMapper::mapEntry)
        }
    }

    override fun getLibraryEntriesAsFlow(): Flow<List<Entry>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            getLibraryEntriesAsFlow(profileId)
        }
    }

    override fun getLibraryEntriesAsFlow(profileId: Long): Flow<List<Entry>> {
        return handler.subscribeToList {
            entriesQueries.getFavorites(profileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getLibraryLastRead(): Map<Long, Long> {
        return getLibraryLastRead(profileProvider.activeProfileId)
    }

    override suspend fun getLibraryLastRead(profileId: Long): Map<Long, Long> {
        return handler.awaitList {
            libraryViewQueries.libraryLastRead(profileId) { entryId, lastRead ->
                entryId to lastRead
            }
        }.toMap()
    }

    override fun observeLibraryLastRead(profileId: Long): Flow<Map<Long, Long>> {
        return handler.subscribeToList {
            libraryViewQueries.libraryLastRead(profileId) { entryId, lastRead ->
                entryId to lastRead
            }
        }.map(List<Pair<Long, Long>>::toMap)
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entriesQueries.getFavoritesBySourceId(profileId, sourceId, EntryMapper::mapEntry)
            }
        }
    }

    override suspend fun getUpcomingEntries(
        profileId: Long,
        statuses: Set<Int>,
        types: Set<EntryType>,
        excludedCategories: List<Long>,
        includedCategories: List<Long>,
    ): Flow<List<Entry>> {
        val timeZone = TimeZone.currentSystemDefault()
        val epochMillis = Clock.System.now()
            .toLocalDateTime(timeZone)
            .date
            .atStartOfDayIn(timeZone)
            .toEpochMilliseconds()
        return handler.subscribeToList {
            entriesQueries.getUpcomingEntries(
                profileId = profileId,
                startOfDay = epochMillis,
                statuses = statuses.map { it.toLong() },
                types = types.map { it.name.lowercase() },
                includedEmpty = includedCategories.isEmpty(),
                includedCategories = includedCategories,
                excludedEmpty = excludedCategories.isEmpty(),
                excludedCategories = excludedCategories,
                mapper = EntryMapper::mapEntry,
            )
        }
    }

    override suspend fun resetViewerFlags(): Boolean {
        return try {
            handler.await { entriesQueries.resetViewerFlags(profileProvider.activeProfileId) }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun setCategories(entryId: Long, categoryIds: List<Long>) {
        handler.await(inTransaction = true) {
            entries_categoriesQueries.deleteByEntryId(profileProvider.activeProfileId, entryId)
            categoryIds.forEach { categoryId ->
                entries_categoriesQueries.insert(profileProvider.activeProfileId, entryId, categoryId)
            }
        }
    }

    override suspend fun updateDisplayName(entryId: Long, displayName: String?): Boolean {
        return try {
            handler.await { entriesQueries.updateDisplayName(displayName, entryId, profileProvider.activeProfileId) }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateNotes(entryId: Long, profileId: Long, notes: String): Boolean {
        return try {
            handler.awaitOneExecutable { entriesQueries.updateNotes(notes, entryId, profileId) } > 0
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun insert(entry: Entry): Long {
        return handler.await(inTransaction = true) {
            entriesQueries.insertReturningId(
                profileId = profileProvider.activeProfileId,
                source = entry.source,
                url = entry.url,
                title = entry.title,
                displayName = entry.displayName,
                artist = entry.artist,
                author = entry.author,
                description = entry.description,
                genre = entry.genre,
                status = entry.status.value.toLong(),
                thumbnailUrl = entry.thumbnailUrl,
                favorite = entry.favorite,
                lastUpdate = entry.lastUpdate,
                nextUpdate = entry.nextUpdate,
                initialized = entry.initialized,
                viewerFlags = entry.viewerFlags,
                chapterFlags = entry.chapterFlags,
                coverLastModified = entry.coverLastModified,
                dateAdded = entry.dateAdded,
                updateStrategy = entry.updateStrategy,
                calculateInterval = entry.fetchInterval.toLong(),
                version = entry.version,
                notes = entry.notes,
                memo = entry.memo,
                type = entry.type.name.lowercase(),
                libraryPinned = entry.favorite && entry.libraryPinned,
            ).awaitAsOne()
        }
    }

    override suspend fun insertOrUpdate(entry: Entry): Entry {
        return insertOrUpdate(entry, profileProvider.activeProfileId)
    }

    override suspend fun insertOrUpdate(entry: Entry, profileId: Long): Entry {
        return handler.await(inTransaction = true) {
            insertOrUpdateNetworkEntry(entry, profileId)
        }
    }

    override suspend fun insertOrUpdateBatch(entries: List<Entry>, profileId: Long): List<Entry> {
        if (entries.isEmpty()) return emptyList()

        val callerContext = currentCoroutineContext()
        val outcome = withContext(NonCancellable) {
            handler.await(inTransaction = true) {
                val persisted = ArrayList<Entry>(entries.size)
                var failure: Throwable? = null
                for (entry in entries) {
                    try {
                        callerContext.ensureActive()
                    } catch (error: Throwable) {
                        failure = error
                        break
                    }

                    var savepointStarted = false
                    try {
                        entriesQueries.beginNetworkEntryPersistence()
                        savepointStarted = true
                        val persistedEntry = insertOrUpdateNetworkEntry(entry, profileId)
                        callerContext.ensureActive()
                        entriesQueries.finishNetworkEntryPersistence()
                        savepointStarted = false
                        persisted += persistedEntry
                    } catch (error: Throwable) {
                        if (savepointStarted) {
                            entriesQueries.rollbackNetworkEntryPersistence()
                            entriesQueries.finishNetworkEntryPersistence()
                        }
                        failure = error
                        break
                    }
                }
                EntryBatchPersistenceOutcome(persisted, failure)
            }
        }
        outcome.failure?.let { throw it }
        return outcome.entries
    }

    private suspend fun Database.insertOrUpdateNetworkEntry(entry: Entry, profileId: Long): Entry {
        entriesQueries.insertNetworkEntry(
            profileId = profileId,
            source = entry.source,
            url = entry.url,
            title = entry.title,
            artist = entry.artist,
            author = entry.author,
            description = entry.description,
            genre = entry.genre,
            status = entry.status.value.toLong(),
            thumbnailUrl = entry.thumbnailUrl,
            favorite = entry.favorite,
            lastUpdate = entry.lastUpdate,
            nextUpdate = entry.nextUpdate,
            initialized = entry.initialized,
            viewerFlags = entry.viewerFlags,
            chapterFlags = entry.chapterFlags,
            coverLastModified = entry.coverLastModified,
            dateAdded = entry.dateAdded,
            updateStrategy = entry.updateStrategy,
            calculateInterval = entry.fetchInterval.toLong(),
            version = entry.version,
            memo = entry.memo,
            type = entry.type.name.lowercase(),
            libraryPinned = entry.favorite && entry.libraryPinned,
        )
        entriesQueries.updateNetworkEntry(
            profileId = profileId,
            source = entry.source,
            url = entry.url,
            title = entry.title,
            artist = entry.artist,
            author = entry.author,
            description = entry.description,
            genre = entry.genre,
            status = entry.status.value.toLong(),
            thumbnailUrl = entry.thumbnailUrl,
            updateStrategy = entry.updateStrategy,
            memo = entry.memo,
            type = entry.type.name.lowercase(),
            updateTitle = entry.title.isNotBlank(),
            updateCover = !entry.thumbnailUrl.isNullOrBlank(),
            updateDetails = entry.initialized,
        )
        return entriesQueries.getEntryByUrlAndSource(
            profileId = profileId,
            url = entry.url,
            source = entry.source,
            type = entry.type.name.lowercase(),
            mapper = EntryMapper::mapEntry,
        ).awaitAsOne()
    }

    override suspend fun update(entry: Entry): Boolean {
        return update(entry, profileProvider.activeProfileId)
    }

    override suspend fun update(entry: Entry, profileId: Long): Boolean {
        return try {
            partialUpdate(entry, profileId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateFromSource(entry: Entry): Boolean {
        return try {
            insertOrUpdate(entry)
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun updateFromSourceSync(
        entry: Entry,
        profileId: Long,
        updateDateAdded: Boolean,
    ): Entry? {
        return handler.await(inTransaction = true) {
            entriesQueries.updateFromSourceSync(
                title = entry.title,
                artist = entry.artist,
                author = entry.author,
                description = entry.description,
                genre = entry.genre,
                status = entry.status.value.toLong(),
                thumbnailUrl = entry.thumbnailUrl,
                lastUpdate = entry.lastUpdate,
                nextUpdate = entry.nextUpdate,
                initialized = entry.initialized,
                coverLastModified = entry.coverLastModified,
                updateDateAdded = updateDateAdded.toLong(),
                dateAdded = entry.dateAdded,
                updateStrategy = entry.updateStrategy,
                calculateInterval = entry.fetchInterval.toLong(),
                memo = entry.memo,
                entryId = entry.id,
                profileId = profileId,
                mapper = EntryMapper::mapEntry,
            ).awaitAsOneOrNull()
        }
    }

    override suspend fun setViewerFlags(id: Long, viewerFlags: Long): Boolean {
        return updateField(id) { copy(viewerFlags = viewerFlags) }
    }

    override suspend fun setChapterFlags(id: Long, flags: Long): Boolean {
        return updateField(id) { copy(chapterFlags = flags) }
    }

    override suspend fun setUpdateStrategy(id: Long, strategy: EntryUpdateStrategy): Boolean {
        return updateField(id) { copy(updateStrategy = strategy) }
    }

    override suspend fun setLibraryPinned(profileId: Long, entryIds: List<Long>, libraryPinned: Boolean) {
        val distinctEntryIds = entryIds.distinct()
        if (distinctEntryIds.isEmpty()) return
        handler.await(inTransaction = true) {
            distinctEntryIds.chunkedForSqlQuery().forEach { entryIdChunk ->
                entriesQueries.setLibraryPinnedForProfile(libraryPinned, profileId, entryIdChunk)
            }
        }
    }

    private suspend fun updateField(id: Long, transform: Entry.() -> Entry): Boolean {
        val entry = getEntryById(id) ?: return false
        return try {
            partialUpdate(transform(entry), profileProvider.activeProfileId)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(entry: Entry, profileId: Long): Boolean {
        return handler.await {
            entriesQueries.update(
                source = entry.source,
                url = entry.url,
                title = entry.title,
                displayName = entry.displayName,
                artist = entry.artist,
                author = entry.author,
                description = entry.description,
                genre = entry.genre?.let(StringListColumnAdapter::encode),
                status = entry.status.value.toLong(),
                thumbnailUrl = entry.thumbnailUrl,
                favorite = entry.favorite,
                lastUpdate = entry.lastUpdate,
                nextUpdate = entry.nextUpdate,
                initialized = entry.initialized,
                viewer = entry.viewerFlags,
                chapterFlags = entry.chapterFlags,
                coverLastModified = entry.coverLastModified,
                dateAdded = entry.dateAdded,
                updateStrategy = UpdateStrategyColumnAdapter.encode(entry.updateStrategy),
                calculateInterval = entry.fetchInterval.toLong(),
                version = entry.version,
                isSyncing = entry.isSyncing,
                notes = entry.notes,
                memo = MemoColumnAdapter.encode(entry.memo),
                type = entry.type.name.lowercase(),
                libraryPinned = entry.favorite && entry.libraryPinned,
                entryId = entry.id,
                profileId = profileId,
            ) > 0L
        }
    }

    override suspend fun getCoverHash(entryId: Long, coverLastModified: Long): Long? {
        return handler.awaitOneOrNull {
            entry_cover_hashesQueries.getCoverHash(
                entryId = entryId,
                profileId = profileProvider.activeProfileId,
                coverLastModified = coverLastModified,
            )
        }
    }

    override suspend fun upsertCoverHash(entryId: Long, coverLastModified: Long, hash: Long) {
        handler.await {
            entry_cover_hashesQueries.upsertCoverHash(
                entryId = entryId,
                profileId = profileProvider.activeProfileId,
                coverLastModified = coverLastModified,
                hash = hash,
            )
        }
    }
}

private data class EntryBatchPersistenceOutcome(
    val entries: List<Entry>,
    val failure: Throwable?,
)
