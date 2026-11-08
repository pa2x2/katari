package app.katari.data.di

import app.cash.sqldelight.db.SqlDriver
import app.katari.data.database.DatabaseRepositoryImpl
import app.katari.data.database.TemporarySchema
import app.katari.domain.database.repository.DatabaseRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.data.AndroidDatabaseHandler
import tachiyomi.data.Chapters
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.data.DateColumnAdapter
import tachiyomi.data.Entries
import tachiyomi.data.History
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.data.UpdateStrategyColumnAdapter

/**
 * The database every platform opens the same way. Each platform's graph adds the [SqlDriver] for its own database
 * file, built with `createSqlDriver` from the [DatabaseRepositoryImpl] and [TemporarySchema] provided here.
 */
@BindingContainer
interface DatabaseBindings {
    @Binds
    val DatabaseRepositoryImpl.databaseRepository: DatabaseRepository

    @Binds
    val AndroidDatabaseHandler.databaseHandler: DatabaseHandler

    companion object {
        // The driver reports its migration state to the repository, which reads the driver back lazily.
        @Provides
        @SingleIn(AppScope::class)
        fun databaseRepositoryImpl(driver: Lazy<SqlDriver>): DatabaseRepositoryImpl = DatabaseRepositoryImpl(driver)

        @Provides
        @SingleIn(AppScope::class)
        fun temporarySchema(): TemporarySchema = TemporarySchema()

        @Provides
        @SingleIn(AppScope::class)
        fun database(driver: SqlDriver): Database = Database(
            driver = driver,
            entriesAdapter = Entries.Adapter(
                genreAdapter = StringListColumnAdapter,
                update_strategyAdapter = UpdateStrategyColumnAdapter,
                memoAdapter = MemoColumnAdapter,
            ),
            chaptersAdapter = Chapters.Adapter(
                memoAdapter = MemoColumnAdapter,
            ),
            historyAdapter = History.Adapter(
                last_readAdapter = DateColumnAdapter,
            ),
        )

        @Provides
        @SingleIn(AppScope::class)
        fun databaseHandler(database: Database, driver: SqlDriver): AndroidDatabaseHandler =
            AndroidDatabaseHandler(database, driver)
    }
}
