package app.katari.desktop.work

import app.katari.core.metro.AppCoroutineScope
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

/**
 * Starts the work the previous launch left pending again, in the order it was queued, and sets up the schedules it
 * kept, as WorkManager does on Android when the process starts. The launch starts it once the profile is open and the
 * extension host runs, since the work reads both.
 */
@Inject
class PersistedWorkResume(
    private val persisted: PersistedWork,
    private val resumable: Set<ResumableWork>,
    @AppCoroutineScope private val scope: CoroutineScope,
) {
    fun start() {
        scope.launch {
            val (pending, schedules) = persisted.takeFromLastLaunch()
            val byKind = resumable.flatMap { work -> work.kinds.map { it to work } }.toMap()
            schedules.values.forEach { schedule -> resume(byKind, schedule.kind, schedule.input) }
            pending.forEach { work -> resume(byKind, work.kind, work.input) }
        }
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
