package eu.kanade.tachiyomi.ui.base.migration

import mihon.domain.database.repository.DatabaseRepository

interface AppMigratingGraph {
    val databaseRepository: DatabaseRepository
}
