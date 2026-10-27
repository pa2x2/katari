package mihon.entry.interactions.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import logcat.LogPriority
import mihon.core.metro.metroGraph
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences

class EntryDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val dependencies by lazy {
        applicationContext.metroGraph<EntryDownloadWorkerGraph>().entryDownloadWorkerDependencies
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationProvider = dependencies.notificationProvider
        return ForegroundInfo(
            notificationProvider.notificationId,
            notificationProvider.notification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    override suspend fun doWork(): Result {
        while (true) {
            if (!dependencies.downloads.hasPendingDownloads()) return Result.success()
            setForegroundSafely(dependencies.notificationProvider)
            allowedNetworkFlow(dependencies.downloadPreferences).first { it }
            if (runUntilNetworkBlocked(dependencies)) return Result.success()
        }
    }

    private suspend fun runUntilNetworkBlocked(
        dependencies: EntryDownloadWorkerDependencies,
    ): Boolean = coroutineScope {
        val processing = async { dependencies.downloads.runDownloadsUntilIdle() }
        val networkBlocked = async { allowedNetworkFlow(dependencies.downloadPreferences).first { !it } }
        select {
            processing.onAwait {
                networkBlocked.cancelAndJoin()
                true
            }
            networkBlocked.onAwait {
                processing.cancelAndJoin()
                false
            }
        }
    }

    private fun allowedNetworkFlow(downloadPreferences: DownloadPreferences): Flow<Boolean> = combine(
        applicationContext.entryDownloadNetworkStateFlow(),
        flow {
            emit(downloadPreferences.downloadOnlyOverWifi.get())
            emitAll(downloadPreferences.downloadOnlyOverWifi.changes())
        },
    ) { network, requireWifi ->
        isEntryDownloadNetworkAllowed(network.isOnline, network.isWifi, requireWifi)
    }

    private suspend fun setForegroundSafely(notificationProvider: EntryDownloadForegroundNotificationProvider) {
        try {
            setForeground(
                ForegroundInfo(
                    notificationProvider.notificationId,
                    notificationProvider.notification(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    } else {
                        0
                    },
                ),
            )
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to foreground download worker" }
        }
    }
}
