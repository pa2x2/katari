package mihon.data.database

import android.app.ActivityManager
import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.eygraber.sqldelight.androidx.driver.AndroidxSqliteDatabaseType
import com.eygraber.sqldelight.androidx.driver.FileProvider

/** Low-RAM devices get one reader instead of four. */
fun createSqlDriver(
    context: Context,
    databaseRepository: DatabaseRepositoryImpl,
    temporarySchema: TemporarySchema,
): SqlDriver {
    val isLowRam = context.getSystemService(ActivityManager::class.java)?.isLowRamDevice == true
    return configuredSqlDriver(
        databaseType = AndroidxSqliteDatabaseType.FileProvider(context, "tachiyomi.db"),
        readerCount = if (isLowRam) 1 else 4,
        databaseRepository = databaseRepository,
        temporarySchema = temporarySchema,
    )
}
