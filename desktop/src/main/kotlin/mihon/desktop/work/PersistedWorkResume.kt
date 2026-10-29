package mihon.desktop.work

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import logcat.LogPriority
import mihon.core.metro.LaunchTask
import tachiyomi.core.common.util.system.logcat

/**
 * Starts the work the previous launch left pending again, in the order it was queued, and sets up the schedules it
 * kept, as WorkManager does on Android after a restart. It runs once the profile is open, since the work reads it.
 */
@Inject
@ContributesIntoSet(AppScope::class)
class PersistedWorkResume(
    private val persisted: PersistedWork,
    private val resumable: Set<ResumableWork>,
) : LaunchTask {
    override suspend fun run(scope: CoroutineScope) {
        val (pending, schedules) = persisted.takeFromLastLaunch()
        val byKind = resumable.flatMap { work -> work.kinds.map { it to work } }.toMap()
        schedules.values.forEach { schedule -> resume(byKind, schedule.kind, schedule.input) }
        pending.forEach { work -> resume(byKind, work.kind, work.input) }
    }

    private suspend fun resume(byKind: Map<String, ResumableWork>, kind: String, input: Map<String, String>) {
        val work = byKind[kind]
        if (work == null) {
            logcat(LogPriority.WARN) { "No work of kind $kind to resume" }
            return
        }
        work.resume(kind, input)
    }
}
