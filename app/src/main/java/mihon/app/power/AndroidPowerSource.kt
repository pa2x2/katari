package mihon.app.power

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.core.platform.power.PowerSource

/** The charger and charge as the battery broadcast reports them: any plug type counts. */
@Inject
class AndroidPowerSource(private val context: Context) : PowerSource {
    override fun isPluggedIn(): Flow<Boolean> =
        batteryChanges { intent -> intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0 }

    override fun batteryPercent(): Flow<Int?> = batteryChanges { intent ->
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) level * PERCENT / scale else null
    }

    private fun <T> batteryChanges(read: (Intent) -> T): Flow<T> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(read(intent))
            }
        }
        // The battery broadcast is sticky, so registering delivers the current state straight away.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { context.unregisterReceiver(receiver) }
    }.distinctUntilChanged()
}

private const val PERCENT = 100
