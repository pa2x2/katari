package mihon.entry.interactions.book.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Date

/** The time as the device's clock shows it, in its 12 or 24 hour format, updated as each minute starts. */
@Composable
internal fun rememberDeviceTime(): State<String> {
    val context = LocalContext.current
    val time = remember { mutableStateOf(context.formattedTime()) }
    DisposableEffect(context) {
        time.value = context.formattedTime()
        receiveSystemBroadcasts(
            context,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        ) { time.value = context.formattedTime() }
    }
    return time
}

/** The battery's charge in percent; null until the system reports it. */
@Composable
internal fun rememberBatteryPercent(): State<Int?> {
    val context = LocalContext.current
    val percent = remember { mutableStateOf<Int?>(null) }
    DisposableEffect(context) {
        // The battery broadcast is sticky, so registering delivers the current charge straight away.
        receiveSystemBroadcasts(context, Intent.ACTION_BATTERY_CHANGED) { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) percent.value = level * PERCENT / scale
        }
    }
    return percent
}

private fun Context.formattedTime(): String = DateFormat.getTimeFormat(this).format(Date())

private fun DisposableEffectScope.receiveSystemBroadcasts(
    context: Context,
    vararg actions: String,
    onReceive: (Intent) -> Unit,
): DisposableEffectResult {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = onReceive(intent)
    }
    val filter = IntentFilter().apply { actions.forEach(::addAction) }
    ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    return onDispose { context.unregisterReceiver(receiver) }
}

private const val PERCENT = 100
