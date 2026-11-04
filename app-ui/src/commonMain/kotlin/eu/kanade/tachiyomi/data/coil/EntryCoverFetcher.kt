package eu.kanade.tachiyomi.data.coil

import coil3.Extras
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.getOrDefault
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.EntryCoverFetcher.Companion.USE_CUSTOM_COVER_KEY
import eu.kanade.tachiyomi.network.await
import logcat.LogPriority
import mihon.entry.interactions.source.EntryCoverNetworkFeature
import mihon.entry.interactions.source.EntryCoverNetworkResolution
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryCover
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * A [Fetcher] that fetches cover image for [Entry] object.
 *
 * It uses [Entry.thumbnailUrl] if custom cover is not set by the user.
 * Disk caching for library items is handled by [CoverCache], otherwise
 * handled by Coil's [DiskCache].
 *
 * Available request parameter:
 * - [USE_CUSTOM_COVER_KEY]: Use custom cover if set by user, default is true
 */
class EntryCoverFetcher(
    private val url: String?,
    private val isLibraryEntry: Boolean,
    private val options: Options,
    private val coverFileLazy: Lazy<File?>,
    private val customCoverFileLazy: Lazy<File>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val sourceNetworkResolutionLazy: Lazy<EntryCoverNetworkResolution>,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val storageFiles: StorageFileResolver,
    private val imageLoader: ImageLoader,
) : Fetcher {

    private val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    override suspend fun fetch(): FetchResult {
        // Use custom cover if exists, which only library entries can have
        val useCustomCover = options.extras.getOrDefault(USE_CUSTOM_COVER_KEY)
        if (useCustomCover && isLibraryEntry) {
            validCoverCacheFile(customCoverFileLazy.value)?.let {
                return fileLoader(it)
            }
        }

        // diskCacheKey is thumbnail_url
        if (url == null) error("No cover specified")
        return when (getResourceType(url)) {
            Type.File -> fileLoader(coverFile(url))
            Type.URI -> fileUriLoader(url)
            // Requests for the same cover at different sizes or through Entry and EntryCover would each download it
            Type.URL -> concurrentRequests.apply(diskCacheKey) { httpLoader() }
            null -> error("Invalid image")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKey,
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    // A content URI from an Android backup opens nowhere on desktop, so its cover shows the placeholder.
    private fun fileUriLoader(uri: String): FetchResult {
        val file = storageFiles.fromUri(uri) ?: throw IOException("No storage opens the cover $uri")
        val source = file.openInputStream().source().buffer()
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = FileSystem.SYSTEM),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun httpLoader(): FetchResult {
        // Only cache separately if it's a library item
        val libraryCoverCacheFile = if (isLibraryEntry) {
            coverFileLazy.value ?: error("No cover specified")
        } else {
            null
        }
        if (options.diskCachePolicy.readEnabled) {
            validCoverCacheFile(libraryCoverCacheFile)?.let {
                return fileLoader(it)
            }
        }

        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, libraryCoverCacheFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                return SourceFetchResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, libraryCoverCacheFile)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceFetchResult(
                    source = ImageSource(source = responseBody.source(), fileSystem = FileSystem.SYSTEM),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceNetworkAccess()?.callFactory ?: callFactoryLazy.value
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HTTP_NOT_MODIFIED) {
            response.close()
            throw IOException(response.message)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder().apply {
            url(url!!)

            sourceNetworkAccess()?.headers?.let {
                headers(it)
            }
        }

        when {
            options.networkCachePolicy.readEnabled -> {
                // don't take up okhttp cache
                request.cacheControl(CACHE_CONTROL_NO_STORE)
            }
            else -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    private fun sourceNetworkAccess(): EntryCoverNetworkResolution.Available? {
        return when (val resolution = sourceNetworkResolutionLazy.value) {
            is EntryCoverNetworkResolution.Available -> resolution
            is EntryCoverNetworkResolution.Failed -> throw resolution.cause
            is EntryCoverNetworkResolution.Missing,
            is EntryCoverNetworkResolution.Unsupported,
            -> null
        }
    }

    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            imageLoader.diskCache?.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            validCoverCacheFile(cacheFile)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            validCoverCacheFile(cacheFile)
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Failed to write response data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
            if (cacheFile.length() == 0L) {
                throw IOException("Empty cover cache file")
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    private fun validCoverCacheFile(file: File?): File? {
        if (file == null || !file.exists()) return null
        if (file.length() > 0L) return file

        file.delete()
        return null
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            imageLoader.diskCache?.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    private fun writeToDiskCache(
        response: Response,
    ): DiskCache.Snapshot? {
        val diskCache = imageLoader.diskCache
        val editor = diskCache?.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                response.body.source().readAll(this)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    /**
     * A `file://` cover is a URI with its path percent-encoded, as the local source writes the covers in plain folders;
     * text that only starts like one is taken as a path.
     */
    private fun coverFile(cover: String): File {
        if (!cover.startsWith("file://")) return File(cover)
        return try {
            File(URI(cover))
        } catch (_: URISyntaxException) {
            File(cover.substringAfter("file://"))
        } catch (_: IllegalArgumentException) {
            File(cover.substringAfter("file://"))
        }
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            cover.startsWith("content") -> Type.URI
            else -> null
        }
    }

    private enum class Type {
        File,
        URI,
        URL,
    }

    class EntryFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val coverCacheLazy: Lazy<CoverCache>,
        private val coverNetworkFeatureLazy: Lazy<EntryCoverNetworkFeature>,
        private val storageFiles: StorageFileResolver,
    ) : Fetcher.Factory<Entry> {

        override fun create(data: Entry, options: Options, imageLoader: ImageLoader): Fetcher {
            return EntryCoverFetcher(
                url = data.thumbnailUrl,
                isLibraryEntry = data.favorite,
                options = options,
                coverFileLazy = lazy { coverCacheLazy.value.getCoverFile(data.thumbnailUrl) },
                customCoverFileLazy = lazy { coverCacheLazy.value.getCustomCoverFile(data.id) },
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                sourceNetworkResolutionLazy = lazy { coverNetworkFeatureLazy.value.resolve(data.source) },
                callFactoryLazy = callFactoryLazy,
                storageFiles = storageFiles,
                imageLoader = imageLoader,
            )
        }
    }

    class EntryCoverFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
        private val coverCacheLazy: Lazy<CoverCache>,
        private val coverNetworkFeatureLazy: Lazy<EntryCoverNetworkFeature>,
        private val storageFiles: StorageFileResolver,
    ) : Fetcher.Factory<EntryCover> {

        override fun create(data: EntryCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return EntryCoverFetcher(
                url = data.url,
                isLibraryEntry = data.isFavorite,
                options = options,
                coverFileLazy = lazy { coverCacheLazy.value.getCoverFile(data.url) },
                customCoverFileLazy = lazy { coverCacheLazy.value.getCustomCoverFile(data.entryId) },
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                sourceNetworkResolutionLazy = lazy { coverNetworkFeatureLazy.value.resolve(data.sourceId) },
                callFactoryLazy = callFactoryLazy,
                storageFiles = storageFiles,
                imageLoader = imageLoader,
            )
        }
    }

    companion object {
        val USE_CUSTOM_COVER_KEY = Extras.Key(true)

        private val CACHE_CONTROL_NO_STORE = CacheControl.Builder().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()

        private val concurrentRequests = DeDupeConcurrentRequests<FetchResult>()

        private const val HTTP_NOT_MODIFIED = 304
    }
}
