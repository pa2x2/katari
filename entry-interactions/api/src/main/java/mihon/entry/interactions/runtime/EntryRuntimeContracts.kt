package mihon.entry.interactions.runtime

import android.app.Activity
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryChapter
import java.io.File

interface EntryPageImageCache {
    val readableSize: String
    fun isImageInCache(imageUrl: String): Boolean
    fun getImageFile(imageUrl: String): File
    fun clear(): Int
}

interface EntryPlayerCache {
    val readableSize: String
    fun clear(): Int
}

fun interface EntryInteractionActivityTheme {
    /** Applies the host application's current theme before the activity creates any UI. */
    fun apply(activity: Activity)
}

interface EntryScanlatorFilterDataSource {
    fun childrenChanged(entryIds: Collection<Long>): Flow<Unit>
    suspend fun children(entryIds: Collection<Long>): List<EntryChapter>
    fun excludedScanlatorsChanged(profileId: Long?, entryIds: Collection<Long>): Flow<Unit>
    suspend fun excludedScanlators(profileId: Long?, entryIds: Collection<Long>): Map<Long, Set<String>>
    suspend fun setExcludedScanlators(profileId: Long?, entryIds: Collection<Long>, excluded: Set<String>)
}
