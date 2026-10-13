package eu.kanade.tachiyomi.ui.reader.loader.scheduling

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether an online chapter may load the whole chapter around the page being read. When the user limits loading
 * ahead to Wi-Fi, mobile data only pays for the pages right around that page.
 */
internal class LoadAheadNetworkPolicy(
    private val context: Context,
) {
    fun allows(onlyOverWifi: Boolean): Boolean {
        if (!onlyOverWifi) return true
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
