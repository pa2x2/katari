package eu.kanade.tachiyomi.data.library

import android.app.Application
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.workDataOf
import app.katari.library.update.planning.LibraryUpdateSchedulePeriod
import app.katari.profiles.core.ProfileDatabase
import app.katari.profiles.core.ProfileStore
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.enqueueUniquePeriodicWorkFromCurrentClock
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_CHARGING
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_NETWORK_NOT_METERED
import tachiyomi.domain.library.service.LibraryPreferences.Companion.DEVICE_ONLY_ON_WIFI
import tachiyomi.domain.library.service.LibraryUpdateSchedule
import java.util.concurrent.TimeUnit

@Inject
class AndroidLibraryUpdateSchedule(
    private val application: Application,
    private val profileDatabase: ProfileDatabase,
    private val profileStore: ProfileStore,
    private val activeProfileProvider: ActiveProfileProvider,
    private val period: LibraryUpdateSchedulePeriod,
) : LibraryUpdateSchedule {
    // The update work waits for them as its constraints.
    override val offersDeviceRestrictions: Boolean = true

    override suspend fun schedule(profileId: Long, prefInterval: Int?) {
        val interval = period.hours(profileId, prefInterval)
        if (interval == null) {
            cancel(profileId)
            return
        }
        val preferences = LibraryPreferences(profileStore.profileStore(profileId))
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

        application.workManager.enqueueUniquePeriodicWorkFromCurrentClock(
            workName(profileId),
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override suspend fun scheduleActiveProfile(prefInterval: Int?) {
        schedule(activeProfileProvider.activeProfileId, prefInterval)
    }

    override suspend fun scheduleAll() {
        application.workManager.cancelUniqueWork(LEGACY_WORK_NAME)
        profileDatabase.getProfiles(includeArchived = true).forEach { profile ->
            schedule(profile.id)
        }
    }

    override fun cancel(profileId: Long) {
        application.workManager.cancelUniqueWork(workName(profileId))
    }

    override fun nextRunFlow(profileId: Long): Flow<Long?> {
        return application.workManager.getWorkInfosForUniqueWorkFlow(workName(profileId))
            .map { workInfos ->
                workInfos
                    .firstOrNull { !it.state.isFinished }
                    ?.nextScheduleTimeMillis
                    ?.takeIf { it != Long.MAX_VALUE }
            }
            .distinctUntilChanged()
    }

    private fun workName(profileId: Long) = "$WORK_NAME_PREFIX$profileId"

    private companion object {
        const val WORK_NAME_PREFIX = "LibraryUpdate-auto-"

        /** The single schedule of versions before schedules were kept per profile. */
        const val LEGACY_WORK_NAME = "LibraryUpdate-auto"
    }
}
