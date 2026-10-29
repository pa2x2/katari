package mihon.desktop.library

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import tachiyomi.domain.library.service.LibraryUpdateSchedule

/**
 * Stands in for automatic library updates, which desktop doesn't run yet: scheduling does nothing and no update is
 * ever due. A schedule that runs the library update queue on desktop replaces this.
 */
@Inject
class NoLibraryUpdateSchedule : LibraryUpdateSchedule {
    override val offersDeviceRestrictions: Boolean = false

    override suspend fun schedule(profileId: Long, prefInterval: Int?) = Unit

    override suspend fun scheduleActiveProfile(prefInterval: Int?) = Unit

    override suspend fun scheduleAll() = Unit

    override fun cancel(profileId: Long) = Unit

    override fun nextRunFlow(profileId: Long): Flow<Long?> = flowOf(null)
}
