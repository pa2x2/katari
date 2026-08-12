package mihon.entry.interactions.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.edit
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
    @Volatile
    private var dependencies: EntryDownloadWorkerDependencies? = null

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notificationProvider = dependencies().notificationProvider
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
        val dependencies = dependencies()
        setForegroundSafely(dependencies.notificationProvider)
        while (true) {
            allowedNetworkFlow(dependencies.downloadPreferences).first { it }
            if (runUntilNetworkBlocked(dependencies)) return Result.success()
            setForegroundSafely(dependencies.notificationProvider)
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

    private suspend fun dependencies(): EntryDownloadWorkerDependencies {
        dependencies?.let { return it }
        EntryDownloadRuntimeAvailability.awaitInstalled()
        return synchronized(this) {
            dependencies ?: EntryDownloadWorkerDependencies(
                downloads = Injekt.get(),
                downloadPreferences = Injekt.get(),
                notificationProvider = Injekt.get(),
            ).also { dependencies = it }
        }
    }
}

private data class EntryDownloadWorkerDependencies(
    val downloads: EntryDownloadRuntimeCoordinator,
    val downloadPreferences: DownloadPreferences,
    val notificationProvider: EntryDownloadForegroundNotificationProvider,
)

internal class DefaultEntryDownloadWorkController(
    private val context: Context,
) : EntryDownloadWorkController {
    override fun start() {
        preferences.edit(commit = true) { putBoolean(KEY_EXECUTION_REQUESTED, true) }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<EntryDownloadJob>()
            .setConstraints(constraints)
            .addTag(TAG)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
    }

    override fun stop() {
        preferences.edit(commit = true) { putBoolean(KEY_EXECUTION_REQUESTED, false) }
        WorkManager.getInstance(context).cancelUniqueWork(TAG)
    }

    override fun resumeIfRequested() {
        if (preferences.getBoolean(KEY_EXECUTION_REQUESTED, true)) start()
    }

    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private companion object {
        const val TAG = "EntryDownloader"
        const val PREFERENCES_NAME = "entry_download_execution"
        const val KEY_EXECUTION_REQUESTED = "requested"
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
