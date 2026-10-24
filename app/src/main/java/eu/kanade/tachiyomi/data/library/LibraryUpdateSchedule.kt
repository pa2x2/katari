package eu.kanade.tachiyomi.data.library

import android.content.Context
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import eu.kanade.tachiyomi.util.system.enqueueUniquePeriodicWorkFromCurrentClock
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import mihon.app.di.appGraph
import mihon.feature.library.update.planning.LibraryUpdateSettings
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import java.util.concurrent.TimeUnit

/**
 * Automatic library updates, one schedule per profile. Each schedule runs a [LibraryAutoUpdateTrigger] at the profile's
 * own interval and device restrictions, which only adds the profile's update to the [LibraryUpdateQueue], so profiles
 * that are due at the same time update one after another.
 */
object LibraryUpdateSchedule {
    private const val WORK_NAME_PREFIX = "LibraryUpdate-auto-"

    /** The single schedule of versions before schedules were kept per profile. */
    private const val LEGACY_WORK_NAME = "LibraryUpdate-auto"

    /**
     * Schedules automatic updates of [profileId] at the shortest interval its library or any category asks for, or
     * cancels them when nothing is updated automatically or the profile is archived.
     *
     * @param prefInterval the library interval when its preference is about to change and doesn't hold it yet.
     */
    suspend fun schedule(context: Context, profileId: Long, prefInterval: Int? = null) {
        val graph = context.appGraph
        val profile = graph.profileDatabase.getProfileById(profileId)
        if (profile == null || profile.isArchived) {
            cancel(context, profileId)
            return
        }
        val preferences = LibraryPreferences(graph.profileStore.profileStore(profileId))
        val interval = LibraryUpdateSettings.schedulePeriodHours(
            intervalHours = prefInterval ?: preferences.autoUpdateInterval.get(),
            categoryRules = graph.libraryUpdateRulesRepository.getCategoryRules(profileId).values,
        )
        if (interval == null) {
            cancel(context, profileId)
            return
        }
        val restrictions = preferences.autoUpdateDeviceRestrictions.get()
        val networkType = if (DEVICE_NETWORK_NOT_METERED in restrictions) {
            NetworkType.UNMETERED
        } else {
            NetworkType.CONNECTED
        }
        val networkRequest = NetworkRequest.Builder().apply {
            removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            if (DEVICE_ONLY_ON_WIFI in restrictions) {
                addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            }
            if (DEVICE_NETWORK_NOT_METERED in restrictions) {
                addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }
        }
            .build()
        val constraints = Constraints.Builder()
            // 'networkRequest' only applies to Android 9+, otherwise 'networkType' is used
            .setRequiredNetworkRequest(networkRequest, networkType)
            .setRequiresCharging(DEVICE_CHARGING in restrictions)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<LibraryAutoUpdateTrigger>(
            interval.toLong(),
            TimeUnit.HOURS,
            10,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
            .setInputData(workDataOf(LibraryAutoUpdateTrigger.KEY_PROFILE to profileId))
            .build()

        context.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
            workName(profileId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /** [schedule] for the active profile, after a screen changed its settings. */
    suspend fun scheduleActiveProfile(context: Context, prefInterval: Int? = null) {
        schedule(context, context.appGraph.activeProfileProvider.activeProfileId, prefInterval)
    }

    /** Brings every profile's schedule in line with its settings, and drops the schedule older versions shared. */
    suspend fun scheduleAll(context: Context) {
        context.workManager.cancelUniqueWork(LEGACY_WORK_NAME)
        context.appGraph.profileDatabase.getProfiles(includeArchived = true).forEach { profile ->
            schedule(context, profile.id)
        }
    }

    fun cancel(context: Context, profileId: Long) {
        context.workManager.cancelUniqueWork(workName(profileId))
    }

    /** When the next automatic update of [profileId] is due to start, or null when none is scheduled. */
    fun nextRunFlow(context: Context, profileId: Long): Flow<Long?> {
        return context.workManager.getWorkInfosForUniqueWorkFlow(workName(profileId))
            .map { workInfos ->
                workInfos
                    .firstOrNull { !it.state.isFinished }
                    ?.nextScheduleTimeMillis
                    ?.takeIf { it != Long.MAX_VALUE }
            }
            .distinctUntilChanged()
    }

    private fun workName(profileId: Long) = "$WORK_NAME_PREFIX$profileId"
}
