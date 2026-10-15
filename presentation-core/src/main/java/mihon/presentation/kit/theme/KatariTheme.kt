package mihon.presentation.kit.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import eu.kanade.domain.ui.model.AppTheme
import mihon.core.metro.metroGraph
import mihon.presentation.kit.theme.colorscheme.BaseColorScheme
import mihon.presentation.kit.theme.colorscheme.CatppuccinColorScheme
import mihon.presentation.kit.theme.colorscheme.GreenAppleColorScheme
import mihon.presentation.kit.theme.colorscheme.LavenderColorScheme
import mihon.presentation.kit.theme.colorscheme.MidnightDuskColorScheme
import mihon.presentation.kit.theme.colorscheme.MonetColorScheme
import mihon.presentation.kit.theme.colorscheme.MonochromeColorScheme
import mihon.presentation.kit.theme.colorscheme.NordColorScheme
import mihon.presentation.kit.theme.colorscheme.StrawberryColorScheme
import mihon.presentation.kit.theme.colorscheme.TachiyomiColorScheme
import mihon.presentation.kit.theme.colorscheme.TakoColorScheme
import mihon.presentation.kit.theme.colorscheme.TealTurqoiseColorScheme
import mihon.presentation.kit.theme.colorscheme.TidalWaveColorScheme
import mihon.presentation.kit.theme.colorscheme.TokyoNightColorScheme
import mihon.presentation.kit.theme.colorscheme.YinYangColorScheme
import mihon.presentation.kit.theme.colorscheme.YotsubaColorScheme

@Composable
fun KatariTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val uiPreferences = remember(context) { context.metroGraph<KatariThemeGraph>().uiPreferences }
    BaseKatariTheme(
        appTheme = appTheme ?: uiPreferences.appTheme.get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
        content = content,
    )
}

@Composable
fun KatariPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseKatariTheme(appTheme, isAmoled, content)

@Composable
private fun BaseKatariTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val colorScheme = remember(appTheme, isDark, isAmoled) {
        getThemeColorScheme(
            context = context,
            appTheme = appTheme,
            isDark = isDark,
            isAmoled = isAmoled,
        )
    }
    val colors = remember(colorScheme, isDark) { KatariColors(colorScheme, isDark) }
    MaterialExpressiveTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalKatariColors provides colors, content = content)
    }
}

/**
 * Everything UI code may style with. Colours, text, spacing, sizes and shapes come from here; the build rejects
 * literals for them outside presentation-core.
 */
object KatariTheme {
    val colors: KatariColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKatariColors.current

    val spacing: KatariSpacing get() = KatariSpacing

    val sizes: KatariSizes get() = KatariSizes

    val shapes: KatariShapes
        @Composable
        @ReadOnlyComposable
        get() = KatariShapes.current()
}

private fun getThemeColorScheme(
    context: Context,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        MonetColorScheme(context)
    } else {
        colorSchemes.getOrDefault(appTheme, TachiyomiColorScheme)
    }
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = appTheme != AppTheme.MONET,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.DEFAULT to TachiyomiColorScheme,
    AppTheme.CATPPUCCIN to CatppuccinColorScheme,
    AppTheme.TOKYONIGHT to TokyoNightColorScheme,
    AppTheme.GREEN_APPLE to GreenAppleColorScheme,
    AppTheme.LAVENDER to LavenderColorScheme,
    AppTheme.MIDNIGHT_DUSK to MidnightDuskColorScheme,
    AppTheme.MONOCHROME to MonochromeColorScheme,
    AppTheme.NORD to NordColorScheme,
    AppTheme.STRAWBERRY_DAIQUIRI to StrawberryColorScheme,
    AppTheme.TAKO to TakoColorScheme,
    AppTheme.TEALTURQUOISE to TealTurqoiseColorScheme,
    AppTheme.TIDAL_WAVE to TidalWaveColorScheme,
    AppTheme.YINYANG to YinYangColorScheme,
    AppTheme.YOTSUBA to YotsubaColorScheme,
)
