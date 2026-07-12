package mihon.feature.profiles.core

import androidx.paging.PagingSource
import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler

class ProfileDatabaseHandler(
    private val delegate: DatabaseHandler,
    private val profileProvider: () -> Long,
    private val profileFlow: Flow<Long>,
) : DatabaseHandler by delegate {

    suspend fun <T> awaitProfile(
        inTransaction: Boolean = false,
        block: suspend Database.(Long) -> T,
    ): T {
        return delegate.await(inTransaction) {
            block(this, profileProvider())
        }
    }

    suspend fun <T : Any> awaitListProfile(
        inTransaction: Boolean = false,
        block: suspend Database.(Long) -> Query<T>,
    ): List<T> {
        return delegate.awaitList(inTransaction) {
            block(this, profileProvider())
        }
    }

    suspend fun <T : Any> awaitOneProfile(
        inTransaction: Boolean = false,
        block: suspend Database.(Long) -> Query<T>,
    ): T {
        return delegate.awaitOne(inTransaction) {
            block(this, profileProvider())
        }
    }

    suspend fun <T : Any> awaitOneExecutableProfile(
        inTransaction: Boolean = false,
        block: suspend Database.(Long) -> ExecutableQuery<T>,
    ): T {
        return delegate.awaitOneExecutable(inTransaction) {
            block(this, profileProvider())
        }
    }

    suspend fun <T : Any> awaitOneOrNullProfile(
        inTransaction: Boolean = false,
        block: suspend Database.(Long) -> Query<T>,
    ): T? {
        return delegate.awaitOneOrNull(inTransaction) {
            block(this, profileProvider())
        }
    }

    fun <T : Any> subscribeToListProfile(block: Database.(Long) -> Query<T>): Flow<List<T>> {
        return profileFlow.flatMapLatest { profileId ->
            delegate.subscribeToList {
                block(this, profileId)
            }
        }
    }

    fun <T : Any> subscribeToOneProfile(block: Database.(Long) -> Query<T>): Flow<T> {
        return profileFlow.flatMapLatest { profileId ->
            delegate.subscribeToOne {
                block(this, profileId)
            }
        }
    }

    fun <T : Any> subscribeToOneOrNullProfile(block: Database.(Long) -> Query<T>): Flow<T?> {
        return profileFlow.flatMapLatest { profileId ->
            delegate.subscribeToOneOrNull {
                block(this, profileId)
            }
        }
    }

    fun <T : Any> subscribeToPagingSourceProfile(
        countQuery: Database.(Long) -> Query<Long>,
        queryProvider: Database.(Long, Long, Long) -> Query<T>,
    ): PagingSource<Long, T> {
        return delegate.subscribeToPagingSource(
            countQuery = { countQuery(this, profileProvider()) },
            queryProvider = { limit, offset -> queryProvider(this, profileProvider(), limit, offset) },
        )
    }
}
