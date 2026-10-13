package eu.kanade.tachiyomi.data.cache

import android.content.Context
import android.system.Os
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryCustomCoverWriter
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Class used to create cover cache.
 * It is used to store the covers of the library.
 * Names of files are created with the md5 of the thumbnail URL.
 *
 * Whether an entry has a custom cover is answered from an index of the custom cover directory, because image keys
 * and list items ask for every visible cover and a disk check each time would block the main thread. The index is
 * only correct while every custom cover is written and deleted through this class.
 *
 * @param context the application context.
 * @constructor creates an instance of the cover cache.
 */
class CoverCache(
    scope: CoroutineScope,
    private val context: Context,
) : EntryCustomCoverWriter {

    companion object {
        private const val COVERS_DIR = "covers"
        private const val CUSTOM_COVERS_DIR = "covers/custom"
    }

    /**
     * Cache directory used for cache management.
     */
    private val cacheDir by lazy { getCacheDir(COVERS_DIR) }

    private val customCoverCacheDir by lazy { getCacheDir(CUSTOM_COVERS_DIR) }

    private val customCoverNames = mutableSetOf<String>()

    // Deleted while the directory was being listed, so the listing may still contain them
    private val deletedWhileIndexing = mutableSetOf<String>()

    @Volatile
    private var customCoversIndexed = false

    init {
        scope.launch {
            val names = customCoverCacheDir.list().orEmpty()
            synchronized(customCoverNames) {
                names.filterNotTo(customCoverNames) { it in deletedWhileIndexing }
                deletedWhileIndexing.clear()
                customCoversIndexed = true
            }
        }
    }

    /**
     * Returns the cover from cache.
     *
     * @param mangaThumbnailUrl thumbnail url for the manga.
     * @return cover image.
     */
    fun getCoverFile(mangaThumbnailUrl: String?): File? {
        return mangaThumbnailUrl?.let {
            File(cacheDir, DiskUtil.hashKeyForDisk(it))
        }
    }

    /**
     * Returns the custom cover from cache.
     *
     * @param entryId the entry id.
     * @return cover image.
     */
    fun getCustomCoverFile(entryId: Long?): File {
        return File(customCoverCacheDir, DiskUtil.hashKeyForDisk(entryId.toString()))
    }

    fun hasCustomCover(entryId: Long?): Boolean {
        val file = getCustomCoverFile(entryId)
        if (!customCoversIndexed) return file.exists()
        return synchronized(customCoverNames) { file.name in customCoverNames }
    }

    /**
     * Saves the given stream as the entry's custom cover to cache.
     *
     * @param entryId the entry id.
     * @param inputStream the stream to copy.
     * @throws IOException if there's any error.
     */
    @Throws(IOException::class)
    fun setCustomCoverToCache(entryId: Long, inputStream: InputStream) {
        val file = getCustomCoverFile(entryId)
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        onCustomCoverWritten(file)
    }

    @Throws(IOException::class)
    fun setCustomCoverToCache(manga: Entry, inputStream: InputStream) {
        setCustomCoverToCache(manga.id, inputStream)
    }

    override fun write(entryId: Long, inputStream: InputStream) {
        setCustomCoverToCache(entryId, inputStream)
    }

    /**
     * Replaces the entry's custom cover in one rename, so a failed copy leaves the previous cover in place.
     *
     * @param temporarySuffix makes the temporary file unique among concurrent replacements of the same cover.
     */
    @Throws(IOException::class)
    fun replaceCustomCover(entryId: Long, inputStream: InputStream, temporarySuffix: String) {
        val file = getCustomCoverFile(entryId)
        val temporary = File(file.parentFile, "${file.name}.$temporarySuffix.tmp")
        try {
            temporary.outputStream().use(inputStream::copyTo)
            Os.rename(temporary.absolutePath, file.absolutePath)
        } finally {
            temporary.delete()
        }
        onCustomCoverWritten(file)
    }

    /**
     * Delete the cover files of the manga from the cache.
     *
     * @param manga the manga.
     * @param deleteCustomCover whether the custom cover should be deleted.
     * @return number of files that were deleted.
     */
    fun deleteFromCache(manga: Entry, deleteCustomCover: Boolean = false): Int {
        var deleted = 0

        getCoverFile(manga.thumbnailUrl)?.let {
            if (it.exists() && it.delete()) ++deleted
        }

        if (deleteCustomCover) {
            if (deleteCustomCover(manga.id)) ++deleted
        }

        return deleted
    }

    /**
     * Delete custom cover of the manga from the cache
     *
     * @param mangaId the manga id.
     * @return whether the cover was deleted.
     */
    fun deleteCustomCover(mangaId: Long?): Boolean {
        val file = getCustomCoverFile(mangaId)
        val deleted = file.exists() && file.delete()
        synchronized(customCoverNames) {
            customCoverNames.remove(file.name)
            if (!customCoversIndexed) deletedWhileIndexing.add(file.name)
        }
        return deleted
    }

    private fun onCustomCoverWritten(file: File) {
        synchronized(customCoverNames) {
            customCoverNames.add(file.name)
            deletedWhileIndexing.remove(file.name)
        }
    }

    private fun getCacheDir(dir: String): File {
        return context.getExternalFilesDir(dir)
            ?: File(context.filesDir, dir).also { it.mkdirs() }
    }
}
