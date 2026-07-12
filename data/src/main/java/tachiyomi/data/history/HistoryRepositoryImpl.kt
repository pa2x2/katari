package tachiyomi.data.history

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.history.model.History
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.history.repository.HistoryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : HistoryRepository {

    override fun getHistory(query: String): Flow<List<HistoryWithRelations>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                historyViewQueries.history(profileId, query, HistoryMapper::mapHistoryWithRelations)
            }
        }
    }

    override suspend fun getLastHistory(): HistoryWithRelations? {
        return handler.awaitOneOrNull {
            historyViewQueries.getLatestHistory(profileProvider.activeProfileId, HistoryMapper::mapHistoryWithRelations)
        }
    }

    override suspend fun getTotalReadDuration(): Long {
        return handler.awaitOne { historyQueries.getReadDuration() }
    }

    override suspend fun getHistoryByEntryId(entryId: Long): List<History> {
        return handler.awaitList {
            historyQueries.getHistoryByEntryId(entryId, HistoryMapper::mapHistory)
        }
    }

    override suspend fun resetHistory(historyId: Long) {
        try {
            handler.await { historyQueries.resetHistoryById(historyId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun resetHistoryByEntryId(entryId: Long) {
        try {
            handler.await { historyQueries.resetHistoryByEntryId(entryId) }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }

    override suspend fun deleteAllHistory(): Boolean {
        return try {
            handler.await { historyQueries.removeAllHistory() }
            true
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
            false
        }
    }

    override suspend fun upsertHistory(historyUpdate: HistoryUpdate) {
        try {
            handler.await(inTransaction = true) {
                val entryId =
                    chaptersQueries.getChapterById(historyUpdate.chapterId).awaitAsOneOrNull()?.entry_id
                        ?: return@await
                historyQueries.upsertUpdate(
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                    historyUpdate.chapterId,
                )
                historyQueries.upsertInsert(
                    entryId,
                    historyUpdate.chapterId,
                    historyUpdate.readAt,
                    historyUpdate.sessionReadDuration,
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, throwable = e)
        }
    }
}
