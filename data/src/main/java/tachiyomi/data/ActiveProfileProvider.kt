package tachiyomi.data

import kotlinx.coroutines.flow.Flow

interface ActiveProfileProvider {
    val activeProfileId: Long
    val activeProfileIdFlow: Flow<Long>
}
