package tachiyomi.data

import kotlinx.coroutines.flow.Flow
import mihon.core.common.platform.PlatformFacility

@PlatformFacility
interface ActiveProfileProvider {
    val activeProfileId: Long
    val activeProfileIdFlow: Flow<Long>
}
