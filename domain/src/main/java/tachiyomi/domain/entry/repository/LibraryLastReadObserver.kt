package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow

interface LibraryLastReadObserver {
    fun observeLibraryLastRead(profileId: Long): Flow<Map<Long, Long>>
}
