package eu.kanade.tachiyomi.ui.library.update.report

import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdates
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

/**
 * Another device's update of the active profile's whole library when it started after this device's latest one. The
 * places that say when the library was last updated name that update then, as the chapters it found reach Updates here
 * too. Updates of part of the library are left out on both devices, so neither hides a newer whole one.
 */
@Inject
class NewerOtherDeviceUpdate(
    private val activeProfileProvider: ActiveProfileProvider,
    private val reportRepository: LibraryUpdateReportRepository,
    private val otherDevices: OtherDeviceLibraryUpdates,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe(): Flow<OtherDeviceLibraryUpdate?> {
        return activeProfileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                otherDevices.subscribe(profileId),
                reportRepository.subscribeLatestRun(profileId),
            ) { other, runHere ->
                val updatedHere = runHere?.takeIf { it.coversLibrary }?.startedAt ?: 0L
                other?.takeIf { it.summary.run.startedAt > updatedHere }
            }
        }
    }
}
