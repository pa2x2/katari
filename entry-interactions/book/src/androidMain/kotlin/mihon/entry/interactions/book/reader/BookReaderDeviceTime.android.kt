package mihon.entry.interactions.book.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.util.Date

/** In the 12 or 24 hour format the device's settings choose. */
@Composable
internal actual fun rememberDeviceTime(): State<String> {
    val context = LocalContext.current
    val time = remember { mutableStateOf(context.formattedTime()) }
    DisposableEffect(context) {
        time.value = context.formattedTime()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                time.value = context.formattedTime()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return time
}

private fun Context.formattedTime(): String = DateFormat.getTimeFormat(this).format(Date())
