package eu.kanade.tachiyomi.ui.video.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.core.content.getSystemService
import app.katari.entry.interactions.reader.settings.ReaderBasePreferences
import app.katari.entry.interactions.runtime.registerEntryInteractionSecureScreen
import app.katari.entry.interactions.runtime.setEntryInteractionContent
import eu.kanade.tachiyomi.core.security.SecurityPreferences

internal fun AppCompatActivity.registerAnimePlayerSecureScreen(
    readerPreferences: ReaderBasePreferences,
    securityPreferences: SecurityPreferences,
) {
    registerEntryInteractionSecureScreen(readerPreferences, securityPreferences)
}

internal inline fun ComponentActivity.setPlayerComposeContent(
    crossinline content: @Composable () -> Unit,
) {
    setEntryInteractionContent { content() }
}

internal fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService<ConnectivityManager>() ?: return false
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    val maxTransport = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 -> NetworkCapabilities.TRANSPORT_LOWPAN
        else -> NetworkCapabilities.TRANSPORT_WIFI_AWARE
    }
    return (NetworkCapabilities.TRANSPORT_CELLULAR..maxTransport).any(networkCapabilities::hasTransport)
}
