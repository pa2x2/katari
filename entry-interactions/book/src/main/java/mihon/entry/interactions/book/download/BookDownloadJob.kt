package mihon.entry.interactions.book.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.selects.select
import logcat.LogPriority
import mihon.entry.interactions.EntryDownloadForegroundNotificationProvider
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class BookDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(
    context,
    workerParams,
) {
    private val manager: BookDownloadManager = Injekt.get()
    private val downloadPreferences: DownloadPreferences = Injekt.get()
    private val notificationProvider: EntryDownloadForegroundNotificationProvider = Injekt.get()

    override suspend fun getForegroundInfo(): ForegroundInfo {
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
        try {
            setForeground(getForegroundInfo())
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to foreground BOOK downloader" }
        }
        while (!isStopped) {
            allowedNetworkFlow().first { it }
            if (runUntilNetworkBlocked()) return Result.success()
            restoreForegroundNotification()
        }
        return Result.success()
    }

    private suspend fun restoreForegroundNotification() {
        try {
            setForeground(getForegroundInfo())
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to restore BOOK downloader foreground state" }
        }
    }

    private suspend fun runUntilNetworkBlocked(): Boolean = coroutineScope {
        val download = async { manager.runDownloads() }
        val networkBlocked = async { allowedNetworkFlow().first { !it } }
        select {
            download.onAwait {
                networkBlocked.cancelAndJoin()
                true
            }
            networkBlocked.onAwait {
                download.cancelAndJoin()
                false
            }
        }
    }

    private fun allowedNetworkFlow(): Flow<Boolean> = combine(
        applicationContext.networkStateFlow(),
        flow {
            emit(downloadPreferences.downloadOnlyOverWifi.get())
            emitAll(downloadPreferences.downloadOnlyOverWifi.changes())
        },
    ) { network, requireWifi ->
        isBookDownloadNetworkAllowed(network.isOnline, network.isWifi, requireWifi)
    }

    companion object {
        private const val TAG = "BookDownloader"

        fun start(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<BookDownloadJob>()
                .setConstraints(constraints)
                .addTag(TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.REPLACE, request)
        }

        fun stop(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(TAG)
        }
    }
}

internal interface BookDownloadWorkController {
    fun start(context: Context)
    fun stop(context: Context)
}

internal object DefaultBookDownloadWorkController : BookDownloadWorkController {
    override fun start(context: Context) = BookDownloadJob.start(context)

    override fun stop(context: Context) = BookDownloadJob.stop(context)
}

internal fun isBookDownloadNetworkAllowed(isOnline: Boolean, isWifi: Boolean, requireWifi: Boolean): Boolean =
    isOnline && (!requireWifi || isWifi)

private data class BookDownloadNetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isWifi: Boolean,
) {
    val isOnline: Boolean = isConnected && isValidated
}

private val Context.connectivityManager: ConnectivityManager
    get() = getSystemService()!!

@Suppress("DEPRECATION")
private fun Context.activeNetworkState(): BookDownloadNetworkState {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return BookDownloadNetworkState(
        isConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false,
        isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
        isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
    )
}

private fun Context.networkStateFlow(): Flow<BookDownloadNetworkState> = callbackFlow {
    val callback = object : NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            trySend(activeNetworkState())
        }

        override fun onLost(network: Network) {
            trySend(activeNetworkState())
        }
    }
    trySend(activeNetworkState())
    connectivityManager.registerDefaultNetworkCallback(callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}
