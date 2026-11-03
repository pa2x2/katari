package mihon.presentation.kit.theme.colorscheme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun rememberMonetColorScheme(): () -> BaseColorScheme {
    val context = LocalContext.current
    return remember(context) { { MonetColorScheme(context) } }
}

private class MonetColorScheme(context: Context) : BaseColorScheme() {

    private val monet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MonetSystemColorScheme(context)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        val seed = WallpaperManager.getInstance(context)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.primaryColor
            ?.toArgb()
        if (seed != null) {
            MonetCompatColorScheme(Color(seed))
        } else {
            TachiyomiColorScheme
        }
    } else {
        TachiyomiColorScheme
    }

    override val darkScheme
        get() = monet.darkScheme

    override val lightScheme
        get() = monet.lightScheme
}

@RequiresApi(Build.VERSION_CODES.S)
private class MonetSystemColorScheme(context: Context) : BaseColorScheme() {
    override val lightScheme = dynamicLightColorScheme(context)
    override val darkScheme = dynamicDarkColorScheme(context)
}
