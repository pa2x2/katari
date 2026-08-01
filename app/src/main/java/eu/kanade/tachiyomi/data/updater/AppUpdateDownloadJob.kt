package eu.kanade.tachiyomi.data.updater

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.util.storage.saveTo
import eu.kanade.tachiyomi.util.system.notificationBuilder
import eu.kanade.tachiyomi.util.system.setForegroundSafely
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.CancellationException
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.i18n.MR
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException

class AppUpdateDownloadJob(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val network: NetworkHelper by injectLazy()

    override suspend fun doWork(): Result {
        val url = inputData.getString(EXTRA_DOWNLOAD_URL)

        if (url.isNullOrEmpty()) {
            return Result.failure()
        }

        return try {
            setForegroundSafely()
            withIOContext { downloadApk(url) }
            Result.success(workDataOf(PROGRESS to 100, EXTRA_DOWNLOAD_URL to url))
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            Result.failure(workDataOf(EXTRA_DOWNLOAD_URL to url))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = context.notificationBuilder(Notifications.CHANNEL_APP_UPDATE)
            .setContentTitle(context.stringResource(MR.strings.update_check_notification_update_available))
            .setContentText(context.stringResource(MR.strings.update_check_notification_download_in_progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()

        return ForegroundInfo(
            Notifications.ID_APP_UPDATER,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    /**
     * Called to start downloading apk of new update
     *
     * @param url url location of file
     */
    private suspend fun downloadApk(url: String) {
        val progressListener = object : ProgressListener {
            // Progress of the download
            var savedProgress = 0

            // Keep track of the last notification sent to avoid posting too many.
            var lastTick = 0L

            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                val progress = calculateProgress(bytesRead, contentLength) ?: return
                val currentTime = System.currentTimeMillis()
                if (progress > savedProgress && currentTime - 200 > lastTick) {
                    savedProgress = progress
                    lastTick = currentTime
                    setProgressAsync(workDataOf(PROGRESS to progress, EXTRA_DOWNLOAD_URL to url))
                }
            }
        }

        val partFile = updatePart(context, id.toString())
        try {
            val response = network.client.newCachelessCallWithProgress(GET(url), progressListener).awaitSuccess()
            response.body.source().saveTo(partFile)

            val apkFile = updateApk(context)
            if ((!apkFile.delete() && apkFile.exists()) || !partFile.renameTo(apkFile)) {
                throw IOException("Unable to replace downloaded update")
            }
            updateUrlFile(context).writeText(url)
        } finally {
            partFile.delete()
        }
    }

    companion object {
        const val TAG = "AppUpdateDownload"

        const val PROGRESS = "progress"

        const val EXTRA_DOWNLOAD_URL = "DOWNLOAD_URL"

        fun updateApk(context: Context): File = File(updateDirectory(context), UPDATE_APK_NAME)

        fun isDownloaded(context: Context, url: String): Boolean {
            return updateApk(context).isFile && updateUrlFile(context).readTextOrNull() == url
        }

        fun start(context: Context, url: String) {
            clearDownloadedUpdate(context)

            val constraints = Constraints(
                requiredNetworkType = NetworkType.CONNECTED,
            )

            val request = OneTimeWorkRequestBuilder<AppUpdateDownloadJob>()
                .setConstraints(constraints)
                .addTag(TAG)
                .addTag(downloadUrlTag(url))
                .setInputData(workDataOf(EXTRA_DOWNLOAD_URL to url))
                .build()

            context.workManager.enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        internal fun downloadUrlFromTags(tags: Set<String>): String? {
            return tags.firstOrNull { it.startsWith(DOWNLOAD_URL_TAG_PREFIX) }
                ?.removePrefix(DOWNLOAD_URL_TAG_PREFIX)
        }

        private fun downloadUrlTag(url: String) = "$DOWNLOAD_URL_TAG_PREFIX$url"

        private fun clearDownloadedUpdate(context: Context) {
            updateApk(context).delete()
            updateUrlFile(context).delete()
            updateDirectory(context).listFiles { file ->
                file.name.startsWith(UPDATE_PART_PREFIX) && file.name.endsWith(UPDATE_PART_SUFFIX)
            }?.forEach(File::delete)
        }

        private fun updateDirectory(context: Context): File = context.externalCacheDir ?: context.cacheDir

        private fun updateUrlFile(context: Context): File = File(updateDirectory(context), UPDATE_URL_NAME)

        private fun updatePart(context: Context, workerId: String): File {
            return File(updateDirectory(context), "$UPDATE_PART_PREFIX$workerId$UPDATE_PART_SUFFIX")
        }

        private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

        private const val DOWNLOAD_URL_TAG_PREFIX = "$TAG:"
        private const val UPDATE_APK_NAME = "update.apk"
        private const val UPDATE_URL_NAME = "update.url"
        private const val UPDATE_PART_PREFIX = "update-"
        private const val UPDATE_PART_SUFFIX = ".part"
    }
}

internal fun calculateProgress(bytesRead: Long, contentLength: Long): Int? {
    if (bytesRead < 0 || contentLength <= 0) return null
    return (bytesRead.toDouble() / contentLength * 100).toInt().coerceIn(0, 99)
}
