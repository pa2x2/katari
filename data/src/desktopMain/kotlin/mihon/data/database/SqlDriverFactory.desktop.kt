package mihon.data.database

import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import java.io.File

fun createSqlDriver(databaseFile: File, databaseRepository: DatabaseRepositoryImpl): SqlDriver {
    return configuredSqlDriver(
        databaseType = AndroidxSqliteDatabaseType.File(databaseFile.path),
        readerCount = 4,
        databaseRepository = databaseRepository,
    )
}
