package mihon.feature.appupdate.download

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import mihon.feature.appupdate.check.ReleaseVersion
import mihon.feature.appupdate.check.UpdateAsset
import okhttp3.CacheControl
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File
import java.io.IOException

/**
 * Downloads update files into `<root>/<version>/`, one version at a time: one downloader per thing updated, such as
 * the Android app and its mpv add-on, each with its own [root]. Keeping the version in the path lets [removeInstalled]
 * tell which downloads the running app has already been updated past.
 */
class UpdateDownloader(
    private val root: File,
    private val network: NetworkHelper,
) {

    /**
     * Returns the downloaded file. A complete earlier download of the same file is reused; the installer checks its
     * digest anyway. [onProgress] gets 0–1, or null while the size is unknown.
     */
    suspend fun download(version: String, asset: UpdateAsset, onProgress: (Float?) -> Unit): File = withIOContext {
        val directory = File(root, version)
        root.listFiles()?.filter { it != directory }?.forEach { it.deleteRecursively() }
        directory.mkdirs()
        directory.listFiles()?.filter { it.name != asset.name }?.forEach { it.deleteRecursively() }

        val file = File(directory, asset.name)
        if (file.isFile && file.length() == asset.size) {
            onProgress(1f)
            return@withIOContext file
        }

        val part = File(directory, "${asset.name}$PART_SUFFIX")
        try {
            downloadTo(part, asset, onProgress)
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (e: IOException) {
            part.delete()
            throw AppUpdateException(AppUpdateError.DownloadFailed(e.message))
        }
        if (!part.renameTo(file)) {
            part.delete()
            throw AppUpdateException(AppUpdateError.DownloadFailed(null))
        }
        file
    }

    fun clear() {
        root.deleteRecursively()
    }

    /** Deletes downloads for versions up to [installed], which are useless once the app runs that version. */
    fun removeInstalled(installed: ReleaseVersion) {
        root.listFiles()
            ?.filter { directory -> ReleaseVersion.parse(directory.name)?.let { it <= installed } ?: true }
            ?.forEach { it.deleteRecursively() }
    }

    private suspend fun downloadTo(part: File, asset: UpdateAsset, onProgress: (Float?) -> Unit) {
        val client = network.client.newBuilder().cache(null).build()
        client.newCall(GET(asset.url, cache = CacheControl.FORCE_NETWORK)).await().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 } ?: asset.size
            var written = 0L
            var reportedPercent = -1
            onProgress(if (total > 0) 0f else null)
            body.source().use { source ->
                part.sink().buffer().use { sink ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = source.read(sink.buffer, BUFFER_SIZE)
                        if (read == -1L) break
                        sink.emitCompleteSegments()
                        written += read
                        if (total > 0) {
                            val percent = (written * 100 / total).toInt().coerceAtMost(100)
                            if (percent != reportedPercent) {
                                reportedPercent = percent
                                onProgress(percent / 100f)
                            }
                        }
                    }
                }
            }
        }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val BUFFER_SIZE = 64L * 1024
    }
}
