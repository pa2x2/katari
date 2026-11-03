package mihon.desktop.di

import app.cash.sqldelight.db.SqlDriver
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.data.database.DatabaseRepositoryImpl
import mihon.data.database.createSqlDriver
import mihon.desktop.storage.DesktopDirectories

@BindingContainer
object DatabaseDriverBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun sqlDriver(directories: DesktopDirectories, databaseRepository: DatabaseRepositoryImpl): SqlDriver =
        createSqlDriver(directories.data.resolve("katari.db").toFile(), databaseRepository)
}
