package mihon.entry.interactions.runtime

import kotlinx.coroutines.flow.Flow
import mihon.core.common.platform.PlatformFacility
import tachiyomi.domain.entry.model.EntryChapter
import java.io.File

/** The reader's chapter page cache, the same store on every platform, which downloads take already-read pages from. */
interface EntryPageImageCache {
    /** Measured on disk, so read it off the main thread. */
    val sizeBytes: Long
    fun isImageInCache(imageUrl: String): Boolean
    fun getImageFile(imageUrl: String): File
    fun clear(): Int
}

@PlatformFacility
interface EntryPlayerCache {
    /** Measured on disk, so read it off the main thread. */
    val sizeBytes: Long
    fun clear(): Int
}

interface EntryScanlatorFilterDataSource {
    fun childrenChanged(entryIds: Collection<Long>): Flow<Unit>
    suspend fun children(entryIds: Collection<Long>): List<EntryChapter>
    fun excludedScanlatorsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit>
    suspend fun excludedScanlators(profileId: Long?, entryIds: Collection<Long>): Map<Long, Set<String>>
    suspend fun setExcludedScanlators(profileId: Long?, entryIds: Collection<Long>, excluded: Set<String>)
}
