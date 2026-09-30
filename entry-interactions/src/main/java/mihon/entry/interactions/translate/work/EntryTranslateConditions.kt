package mihon.entry.interactions.translate.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mihon.entry.interactions.download.entryDownloadNetworkStateFlow
import mihon.entry.interactions.translate.EntryTranslatePreferences
import mihon.entry.interactions.translate.EntryTranslateWaiting
import mihon.entry.interactions.translate.queue.EntryTranslateSetupCodec
import mihon.translation.api.engine.KnownTranslationEngine
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.entry.model.EntryTranslationQueueItem
import tachiyomi.domain.entry.repository.EntryTranslationQueueRepository

/**
 * Whether background translation may run now under the charging and Wi-Fi rules, or what it waits for. The Wi-Fi rule
 * holds the whole queue while any queued chapter is translated by an engine that sends text over the network.
 */
internal class EntryTranslateConditions(
    private val context: Context,
    private val preferences: EntryTranslatePreferences,
    private val repository: EntryTranslationQueueRepository,
    private val engines: () -> List<KnownTranslationEngine>,
) {
    val waiting: Flow<EntryTranslateWaiting?> = combine(
        context.pluggedInFlow(),
        context.entryDownloadNetworkStateFlow().map { it.isWifi }.distinctUntilChanged(),
        preferences.onlyWhileCharging.values(),
        preferences.onlyOverWifi.values(),
        repository.subscribeAll().map(::needsNetwork).distinctUntilChanged(),
    ) { pluggedIn, wifi, onlyWhileCharging, onlyOverWifi, needsNetwork ->
        when {
            onlyWhileCharging && !pluggedIn -> EntryTranslateWaiting.Charger
            onlyOverWifi && needsNetwork && !wifi -> EntryTranslateWaiting.Wifi
            else -> null
        }
    }.distinctUntilChanged()

    /** Whether the user paused the queue. */
    val paused: Flow<Boolean> = preferences.queuePaused.values()

    private fun needsNetwork(items: List<EntryTranslationQueueItem>): Boolean {
        val networkEngines = engines().filter(KnownTranslationEngine::usesNetwork).mapTo(HashSet()) { it.id }
        if (networkEngines.isEmpty()) return false
        return items.any { item ->
            item.state == EntryTranslationQueueItem.State.Queued &&
                item.setup?.let(EntryTranslateSetupCodec::decode)?.engine in networkEngines
        }
    }
}

private fun <T> Preference<T>.values(): Flow<T> = changes().onStart { emit(get()) }.distinctUntilChanged()

/** Whether the device is on a charger, which also counts once the battery is full. */
private fun Context.pluggedInFlow(): Flow<Boolean> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            trySend(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0)
        }
    }
    // The battery broadcast is sticky, so registering delivers the current state straight away.
    ContextCompat.registerReceiver(
        this@pluggedInFlow,
        receiver,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    awaitClose { unregisterReceiver(receiver) }
}.distinctUntilChanged()
