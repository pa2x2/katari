package mihon.entry.interactions

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.Flow
import mihon.entry.viewer.settings.ViewerSettingsProvider
import java.io.File

interface EntryPageImageCache {
    fun isImageInCache(imageUrl: String): Boolean
    fun getImageFile(imageUrl: String): File
}

object EntryMediaCacheBucketKeys {
    const val MANGA_PAGE_IMAGE: String = "manga_page_image"
    const val ANIME_PLAYBACK: String = "anime_playback"
    const val BOOK_MATERIALIZED: String = "book_materialized"
}

interface EntryMediaCacheBucket {
    val key: String
    val readableSize: String
    fun clear(): Int
}

interface EntryMediaCacheMaintenance {
    fun buckets(): List<EntryMediaCacheBucket>
    fun bucket(key: String): EntryMediaCacheBucket?
    fun clear(key: String): Int
}

fun interface EntryInteractionActivityTheme {
    /** Applies the host application's current theme before the activity creates any UI. */
    fun apply(activity: Activity)
}

data class EntryInteractionRuntimeContribution(
    val mediaCacheBuckets: List<EntryMediaCacheBucket> = emptyList(),
    val viewerSettingsProviders: List<ViewerSettingsProvider> = emptyList(),
)

interface EntryReaderIncognitoState {
    fun isIncognito(sourceId: Long?): Boolean
}

interface EntryReaderTracking {
    suspend fun updateChapterRead(context: Context, entryId: Long, chapterNumber: Double)
}

interface EntryPlayerCache : EntryMediaCacheBucket {
    override val key: String
        get() = EntryMediaCacheBucketKeys.ANIME_PLAYBACK
}

interface EntryChildGroupFilterDataSource {
    fun availableGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun availableGroups(entryIds: Collection<Long>): Set<String>
    fun excludedGroupsChanged(entryId: Long): Flow<Unit>
    suspend fun excludedGroups(entryIds: Collection<Long>): Set<String>
    suspend fun setExcludedGroups(entryIds: Collection<Long>, excluded: Set<String>)
}
