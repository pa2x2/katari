package app.katari.data.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.katari.domain.database.repository.DatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class DatabaseRepositoryImpl(
    // Lazy since the driver reports its migration state here
    private val driver: Lazy<SqlDriver>,
) : DatabaseRepository {

    override val isMigrating = MutableStateFlow(false)

    override suspend fun awaitOpen() {
        withContext(Dispatchers.IO) {
            driver.value.executeQuery(null, "SELECT 1", { QueryResult.Value(Unit) }, 0).await()
        }
    }
}
