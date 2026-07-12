package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.EntryUpdateStrategy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.repository.EntryRepository
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : EntryRepository {

    override suspend fun getEntryById(id: Long): Entry? {
        return handler.awaitOneOrNull {
            entriesQueries.getEntryById(id, profileProvider.activeProfileId, EntryMapper::mapEntry)
        }
    }

    override suspend fun getEntryByIdAsFlow(id: Long): Flow<Entry> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull {
                entriesQueries.getEntryById(id, profileId, EntryMapper::mapEntry)
            }.filterNotNull()
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

    override suspend fun getLibraryEntries(): List<Entry> {
        return handler.awaitList {
            libraryViewQueries.library(profileProvider.activeProfileId, EntryMapper::mapLibraryEntry)
        }
    }

    override fun getLibraryEntriesAsFlow(): Flow<List<Entry>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList { libraryViewQueries.library(profileId, EntryMapper::mapLibraryEntry) }
        }
    }

    override suspend fun getLibraryLastRead(): Map<Long, Long> {
        return handler.awaitList {
            libraryViewQueries.libraryLastRead(profileProvider.activeProfileId) { entryId, lastRead ->
                entryId to lastRead
            }
        }.toMap()
    }

    override fun getFavoritesBySourceId(sourceId: Long): Flow<List<Entry>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entriesQueries.getFavoritesBySourceId(profileId, sourceId, EntryMapper::mapEntry)
            }
        }
    }

    override suspend fun getUpcomingEntries(
        statuses: Set<Int>,
        types: Set<EntryType>,
    ): Flow<List<Entry>> {
        val epochMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                entriesQueries.getUpcomingEntries(
                    profileId,
                    epochMillis,
                    statuses.map {
                        it.toLong()
                    },
                    types.map { it.name.lowercase() },
                    EntryMapper::mapEntry,
                )
            }
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
            ).awaitAsOne()
        }
    }

    override suspend fun insertOrUpdate(entry: Entry): Entry {
        return handler.await(inTransaction = true) {
            entriesQueries.insertNetworkEntry(
                profileId = profileProvider.activeProfileId,
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
                updateTitle = entry.title.isNotBlank(),
                updateCover = !entry.thumbnailUrl.isNullOrBlank(),
                updateDetails = entry.initialized,
            ).awaitAsOne().let(EntryMapper::mapEntry)
        }
    }

    override suspend fun update(entry: Entry): Boolean {
        return try {
            partialUpdate(entry)
            true
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

    override suspend fun setFavorite(id: Long, favorite: Boolean): Boolean {
        return updateField(id) { copy(favorite = favorite) }
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

    private suspend fun updateField(id: Long, transform: Entry.() -> Entry): Boolean {
        val entry = getEntryById(id) ?: return false
        return try {
            partialUpdate(transform(entry))
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    private suspend fun partialUpdate(entry: Entry) {
        handler.await {
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
                entryId = entry.id,
                profileId = profileProvider.activeProfileId,
            )
        }
    }

    override suspend fun delete(id: Long): Boolean {
        return try {
            handler.await {
                entriesQueries.deleteById(profileProvider.activeProfileId, id)
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
        }
    }

    override suspend fun deleteNonFavorite(): Boolean {
        return try {
            handler.await(inTransaction = true) {
                val sourceIds = entriesQueries.getSourceIdsWithNonLibraryEntries(
                    profileProvider.activeProfileId,
                ).awaitAsList().map { it.source }
                if (sourceIds.isNotEmpty()) {
                    entriesQueries.deleteNonLibraryEntries(
                        profileProvider.activeProfileId,
                        sourceIds,
                        keepReadEntries = 0,
                    )
                }
            }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
            false
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
