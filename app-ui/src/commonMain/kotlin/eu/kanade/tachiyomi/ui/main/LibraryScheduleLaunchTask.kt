package eu.kanade.tachiyomi.ui.main

import app.katari.core.metro.LaunchTask
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.library.service.LibraryUpdateSchedule

/** Brings every profile's automatic library update schedule in line with its settings at each launch. */
@Inject
@ContributesIntoSet(AppScope::class)
class LibraryScheduleLaunchTask(private val schedule: LibraryUpdateSchedule) : LaunchTask {
    override suspend fun run(scope: CoroutineScope) {
        schedule.scheduleAll()
    }
}
