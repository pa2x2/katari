package app.katari.desktop.di

import app.cash.sqldelight.db.SqlDriver
import app.katari.data.database.DatabaseRepositoryImpl
import app.katari.data.database.TemporarySchema
import app.katari.data.database.createSqlDriver
import app.katari.desktop.storage.DesktopDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object DatabaseDriverBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun sqlDriver(
        directories: DesktopDirectories,
        databaseRepository: DatabaseRepositoryImpl,
        temporarySchema: TemporarySchema,
    ): SqlDriver = createSqlDriver(directories.data.resolve("katari.db").toFile(), databaseRepository, temporarySchema)
}
