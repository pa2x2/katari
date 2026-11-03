package mihon.desktop.work

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import logcat.LogPriority
import mihon.desktop.storage.DesktopDirectories
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * The work [InProcessWork] has that hasn't ended, and when each periodic schedule last ran, kept in the state directory
 * so that the next launch can start the work again and keep the schedules' rhythm. Each change is written at once, as
 * the app may quit at any time.
 */
@Inject
@SingleIn(AppScope::class)
class PersistedWork(directories: DesktopDirectories) {
    private val file = directories.state.resolve("work.json")
    private val pending = LinkedHashMap<String, PendingWork>()
    private val schedules = LinkedHashMap<String, Schedule>()

    /** What the previous launch left pending, until [takeFromLastLaunch] hands it over. */
    private val fromLastLaunch: List<PendingWork>

    init {
        val saved = read()
        fromLastLaunch = saved.pending
        saved.pending.associateByTo(pending, PendingWork::id)
        schedules.putAll(saved.schedules)
    }

    /**
     * The work the previous launch left pending, in the order it was queued, and every schedule it kept. Pending work
     * is handed over once and forgotten, since starting it again records it anew.
     */
    @Synchronized
    fun takeFromLastLaunch(): Pair<List<PendingWork>, Map<String, Schedule>> {
        val taken = fromLastLaunch.filter { pending.remove(it.id) != null }.sortedBy(PendingWork::queuedAt)
        write()
        return taken to schedules.toMap()
    }

    @Synchronized
    fun add(work: PendingWork) {
        pending[work.id] = work
        write()
    }

    @Synchronized
    fun remove(id: String) {
        if (pending.remove(id) != null) write()
    }

    @Synchronized
    fun lastRunAt(scheduleName: String): Long? = schedules[scheduleName]?.lastRunAt

    /** Keeps the schedule [name], with the last run it had. */
    @Synchronized
    fun putSchedule(name: String, kind: String, input: Map<String, String>) {
        schedules[name] = Schedule(kind, input, lastRunAt = schedules[name]?.lastRunAt)
        write()
    }

    @Synchronized
    fun recordScheduledRun(name: String, at: Long) {
        val schedule = schedules[name] ?: return
        schedules[name] = schedule.copy(lastRunAt = at)
        write()
    }

    @Synchronized
    fun removeSchedule(name: String) {
        if (schedules.remove(name) != null) write()
    }

    private fun read(): SavedWork {
        if (!Files.exists(file)) return SavedWork()
        return try {
            json.decodeFromString(SavedWork.serializer(), Files.readString(file))
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't read the work left pending" }
            SavedWork()
        } catch (e: SerializationException) {
            logcat(LogPriority.ERROR, e) { "Can't read the work left pending" }
            SavedWork()
        }
    }

    private fun write() {
        val saved = SavedWork(pending.values.toList(), schedules.toMap())
        try {
            // Written beside the file and moved over it, so quitting while writing leaves the last whole state.
            val temporary = file.resolveSibling("${file.fileName}.tmp")
            Files.writeString(temporary, json.encodeToString(SavedWork.serializer(), saved))
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            logcat(LogPriority.ERROR, e) { "Can't keep the pending work for the next launch" }
        }
    }

    /** Work that hadn't ended, which [ResumableWork] of [kind] starts again with [input]. */
    @Serializable
    data class PendingWork(val id: String, val kind: String, val input: Map<String, String>, val queuedAt: Long)

    /** A periodic schedule, which [ResumableWork] of [kind] sets up again with [input]. */
    @Serializable
    data class Schedule(val kind: String, val input: Map<String, String>, val lastRunAt: Long?)

    @Serializable
    private data class SavedWork(
        val pending: List<PendingWork> = emptyList(),
        val schedules: Map<String, Schedule> = emptyMap(),
    )

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
