package eu.kanade.tachiyomi.ui.base.migration

import app.katari.domain.database.repository.DatabaseRepository

interface AppMigratingGraph {
    val databaseRepository: DatabaseRepository
}
