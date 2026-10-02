package mihon.feature.appupdate.install

import android.content.Context
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.feature.appupdate.AppUpdateError
import mihon.feature.appupdate.AppUpdateException
import mihon.feature.appupdate.check.ReleaseVersion
import mihon.feature.appupdate.check.UpdateApk
import okhttp3.CacheControl
import okio.buffer
import okio.sink
import tachiyomi.core.common.util.lang.withIOContext
import java.io.File
import java.io.IOException

/**
 * Downloads update APKs into `cache/app-update/<version>/`, one version at a time. Keeping the version in the path
 * lets [removeInstalled] tell which downloads the running app has already been updated past.
 */
internal class UpdateApkDownloader(
    private val context: Context,
    private val network: NetworkHelper,
) {

    private val root: File get() = File(context.cacheDir, DIRECTORY)

    /**
     * Returns the downloaded APK. A complete earlier download of the same file is reused; the installer checks its
     * digest anyway. [onProgress] gets 0–1, or null while the size is unknown.
     */
    suspend fun download(version: String, apk: UpdateApk, onProgress: (Float?) -> Unit): File = withIOContext {
        val directory = File(root, version)
        root.listFiles()?.filter { it != directory }?.forEach { it.deleteRecursively() }
        directory.mkdirs()
        directory.listFiles()?.filter { it.name != apk.name }?.forEach { it.deleteRecursively() }

        val file = File(directory, apk.name)
        if (file.isFile && file.length() == apk.size) {
            onProgress(1f)
            return@withIOContext file
        }

        val part = File(directory, "${apk.name}$PART_SUFFIX")
        try {
            downloadTo(part, apk, onProgress)
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
        removeLegacyDownload()
    }

    private suspend fun downloadTo(part: File, apk: UpdateApk, onProgress: (Float?) -> Unit) {
        val client = network.client.newBuilder().cache(null).build()
        client.newCall(GET(apk.url, cache = CacheControl.FORCE_NETWORK)).await().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body
            val total = body.contentLength().takeIf { it > 0 } ?: apk.size
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

    /** Files the WorkManager download of Katari 1.12.0 and earlier left in the external cache. */
    private fun removeLegacyDownload() {
        val directory = context.externalCacheDir ?: context.cacheDir
        directory.listFiles { file ->
            file.name == "update.apk" ||
                file.name == "update.url" ||
                (file.name.startsWith("update-") && file.name.endsWith(".part"))
        }?.forEach(File::delete)
    }

    private companion object {
        const val DIRECTORY = "app-update"
        const val PART_SUFFIX = ".part"
        const val BUFFER_SIZE = 64L * 1024
    }
}
