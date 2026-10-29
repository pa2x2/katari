package mihon.desktop.library

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import logcat.LogPriority
import mihon.desktop.work.InProcessWork
import mihon.desktop.work.InProcessWorkRequest
import mihon.desktop.work.ResumableWork
import mihon.feature.library.update.planning.LibraryUpdateSchedulePeriod
import mihon.feature.profiles.core.ProfileDatabase
import tachiyomi.core.common.util.system.logcat
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryUpdateQueue
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import tachiyomi.domain.library.update.model.LibraryUpdateStart
import tachiyomi.domain.work.WorkOutcome
import kotlin.time.Duration.Companion.hours

/**
 * Each profile's automatic library updates as a periodic [InProcessWork] schedule, which runs while the app is open and
 * runs once at launch when the app was closed while one was due (D019). A due schedule queues the profile's update
 * once online. Desktop can't tell charging or Wi-Fi apart, so it offers no device restrictions.
 */
@Inject
@SingleIn(AppScope::class)
class InProcessLibraryUpdateSchedule(
    private val work: InProcessWork,
    private val period: LibraryUpdateSchedulePeriod,
    private val profileDatabase: ProfileDatabase,
    private val activeProfileProvider: ActiveProfileProvider,
    private val queue: LibraryUpdateQueue,
) : LibraryUpdateSchedule, ResumableWork {
    override val offersDeviceRestrictions: Boolean = false

    override val kinds: Set<String> = setOf(KIND)

    override suspend fun schedule(profileId: Long, prefInterval: Int?) {
        val interval = period.hours(profileId, prefInterval)
        if (interval == null) {
            cancel(profileId)
            return
        }
        work.schedulePeriodic(
            workName(profileId),
            interval.hours,
            InProcessWorkRequest(
                resumeAs = KIND,
                input = mapOf(KEY_PROFILE to profileId.toString()),
                requiresNetwork = true,
            ) {
                if (queue.startAutomatic(profileId) == LibraryUpdateStart.AlreadyQueued) {
                    logcat(LogPriority.INFO) {
                        "Skipping automatic library update of profile $profileId, one is already queued"
                    }
                }
                WorkOutcome.Done
            },
        )
    }

    override suspend fun scheduleActiveProfile(prefInterval: Int?) {
        schedule(activeProfileProvider.activeProfileId, prefInterval)
    }

    override suspend fun scheduleAll() {
        profileDatabase.getProfiles(includeArchived = true).forEach { profile ->
            schedule(profile.id)
        }
    }

    override fun cancel(profileId: Long) {
        work.cancelPeriodic(workName(profileId))
    }

    override fun nextRunFlow(profileId: Long): Flow<Long?> = work.nextRunAt(workName(profileId))

    override suspend fun resume(kind: String, input: Map<String, String>) {
        input[KEY_PROFILE]?.toLongOrNull()?.let { schedule(it) }
    }

    private fun workName(profileId: Long) = "$KIND-$profileId"

    private companion object {
        const val KIND = "LibraryUpdate-auto"
        const val KEY_PROFILE = "profile"
    }
}
