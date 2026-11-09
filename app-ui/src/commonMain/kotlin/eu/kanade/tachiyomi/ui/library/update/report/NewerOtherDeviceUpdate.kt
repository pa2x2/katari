package eu.kanade.tachiyomi.ui.library.update.report

import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdate
import app.katari.sync.client.libraryupdate.OtherDeviceLibraryUpdates
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryPreferences

/**
 * Another device's update of the active profile's whole library when it started after this device last updated any
 * of it. The places that say when the library was last updated name that update then, as the chapters it found reach
 * Updates here too.
 */
@Inject
class NewerOtherDeviceUpdate(
    private val activeProfileProvider: ActiveProfileProvider,
    private val libraryPreferences: LibraryPreferences,
    private val otherDevices: OtherDeviceLibraryUpdates,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun subscribe(): Flow<OtherDeviceLibraryUpdate?> {
        return activeProfileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            combine(
                otherDevices.subscribe(profileId),
                libraryPreferences.lastUpdatedTimestamp.changes(),
            ) { other, updatedHere ->
                other?.takeIf { it.summary.run.startedAt > updatedHere }
            }
        }
    }
}
