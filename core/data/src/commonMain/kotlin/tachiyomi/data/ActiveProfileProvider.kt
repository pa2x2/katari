package tachiyomi.data

import app.katari.core.common.platform.PlatformFacility
import kotlinx.coroutines.flow.Flow

@PlatformFacility
interface ActiveProfileProvider {
    val activeProfileId: Long
    val activeProfileIdFlow: Flow<Long>
}
