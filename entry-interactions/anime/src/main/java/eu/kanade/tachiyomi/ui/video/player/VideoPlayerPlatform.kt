package eu.kanade.tachiyomi.ui.video.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.IntRange
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import mihon.entry.interactions.setEntryInteractionContent
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.abs

internal class AnimePlayerBasePreferences(
    preferenceStore: PreferenceStore,
) {
    val incognitoMode = preferenceStore.getBoolean(Preference.appStateKey("incognito_mode"), false)
    val immersiveFeedMuted = preferenceStore.getBoolean(
        Preference.appStateKey("immersive_feed_muted"),
        true,
    )
}

internal fun AppCompatActivity.registerAnimePlayerSecureScreen() {
    val preferences = Injekt.get<AnimePlayerBasePreferences>()
    val securityPreferences = Injekt.get<SecurityPreferences>()

    combine(
        securityPreferences.secureScreen.changes(),
        preferences.incognitoMode.changes(),
    ) { secureScreen, incognitoMode ->
        secureScreen == SecurityPreferences.SecureScreenMode.ALWAYS ||
            (secureScreen == SecurityPreferences.SecureScreenMode.INCOGNITO && incognitoMode)
    }
        .onEach { enabled ->
            if (enabled) {
                window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
        .launchIn(lifecycleScope)
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

@Composable
internal fun VideoPlayerContentOverlay(
    @IntRange(from = -100, to = 100) brightness: Int,
    @ColorInt color: Int?,
    colorBlendMode: BlendMode?,
    modifier: Modifier = Modifier,
) {
    if (brightness < 0) {
        val brightnessAlpha = remember(brightness) {
            abs(brightness) / 100f
        }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = brightnessAlpha
                },
        ) {
            drawRect(Color.Black)
        }
    }

    if (color != null) {
        Canvas(
            modifier = modifier.fillMaxSize(),
        ) {
            drawRect(
                color = Color(color),
                blendMode = colorBlendMode ?: BlendMode.SrcOver,
            )
        }
    }
}
