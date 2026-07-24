package mihon.entry.interactions

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
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.download.service.DownloadPreferences
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryDownloadJob(context: Context, workerParams: WorkerParameters) : CoroutineWorker(context, workerParams) {
    private val downloads: EntryDownloadRuntimeCoordinator = Injekt.get()
    private val downloadPreferences: DownloadPreferences = Injekt.get()
    private val notificationProvider: EntryDownloadForegroundNotificationProvider = Injekt.get()

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        notificationProvider.notificationId,
        notificationProvider.notification(),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        },
    )

    override suspend fun doWork(): Result {
        setForegroundSafely()
        while (!isStopped) {
            allowedNetworkFlow().first { it }
            if (runUntilNetworkBlocked()) return Result.success()
            setForegroundSafely()
        }
        return Result.success()
    }

    private suspend fun runUntilNetworkBlocked(): Boolean = coroutineScope {
        val processing = async { downloads.runDownloadsUntilIdle() }
        val networkBlocked = async { allowedNetworkFlow().first { !it } }
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

    private fun allowedNetworkFlow(): Flow<Boolean> = combine(
        applicationContext.entryDownloadNetworkStateFlow(),
        flow {
            emit(downloadPreferences.downloadOnlyOverWifi.get())
            emitAll(downloadPreferences.downloadOnlyOverWifi.changes())
        },
    ) { network, requireWifi ->
        isEntryDownloadNetworkAllowed(network.isOnline, network.isWifi, requireWifi)
    }

    private suspend fun setForegroundSafely() {
        try {
            setForeground(getForegroundInfo())
        } catch (error: IllegalStateException) {
            logcat(LogPriority.ERROR, error) { "Not allowed to foreground download worker" }
        }
    }
}

internal class DefaultEntryDownloadWorkController(
    private val context: Context,
) : EntryDownloadWorkController {
    override fun start() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<EntryDownloadJob>()
            .setConstraints(constraints)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TAG, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    override fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(TAG)
    }

    private companion object {
        const val TAG = "EntryDownloader"
    }
}

internal fun isEntryDownloadNetworkAllowed(isOnline: Boolean, isWifi: Boolean, requireWifi: Boolean): Boolean =
    isOnline && (!requireWifi || isWifi)

private data class EntryDownloadNetworkState(
    val isConnected: Boolean,
    val isValidated: Boolean,
    val isWifi: Boolean,
) {
    val isOnline: Boolean = isConnected && isValidated
}

private val Context.connectivityManager: ConnectivityManager
    get() = getSystemService()!!

@Suppress("DEPRECATION")
private fun Context.activeEntryDownloadNetworkState(): EntryDownloadNetworkState {
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return EntryDownloadNetworkState(
        isConnected = connectivityManager.activeNetworkInfo?.isConnected ?: false,
        isValidated = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
        isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false,
    )
}

private fun Context.entryDownloadNetworkStateFlow(): Flow<EntryDownloadNetworkState> = callbackFlow {
    val callback = object : NetworkCallback() {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            trySend(activeEntryDownloadNetworkState())
        }

        override fun onLost(network: Network) {
            trySend(activeEntryDownloadNetworkState())
        }
    }
    trySend(activeEntryDownloadNetworkState())
    connectivityManager.registerDefaultNetworkCallback(callback)
    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}
