package mihon.entry.interactions.manga.reader.text.session

import android.content.Context
import android.net.ConnectivityManager
import android.os.BatteryManager

/**
 * Whether pages the reader has not reached yet may be processed now. Pages on screen are always processed; work
 * ahead waits for an unmetered network or a charger when the user asked for that.
 */
internal class MangaTextProcessAheadPolicy(
    private val context: Context,
) {
    fun allows(onlyOnUnmeteredNetwork: Boolean, onlyWhileCharging: Boolean): Boolean {
        if (onlyOnUnmeteredNetwork) {
            val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
            if (connectivity.isActiveNetworkMetered) return false
        }
        if (onlyWhileCharging) {
            val battery = context.getSystemService(BatteryManager::class.java) ?: return false
            if (!battery.isCharging) return false
        }
        return true
    }
}
