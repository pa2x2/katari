package mihon.desktop.profile

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.feature.profiles.core.ProfileConstants
import tachiyomi.data.ActiveProfileProvider

/**
 * Desktop has no profile switching yet, so the profile the database creates is always the active one. The profile
 * store Android uses replaces this once desktop can create and switch profiles.
 */
@Inject
class DefaultProfileProvider : ActiveProfileProvider {
    override val activeProfileId: Long = ProfileConstants.DEFAULT_PROFILE_ID

    override val activeProfileIdFlow: Flow<Long> = flowOf(ProfileConstants.DEFAULT_PROFILE_ID)
}
