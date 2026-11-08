package app.katari.presentation.kit.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.presentation.kit.theme.colorscheme.BaseColorScheme
import app.katari.presentation.kit.theme.colorscheme.CatppuccinColorScheme
import app.katari.presentation.kit.theme.colorscheme.GreenAppleColorScheme
import app.katari.presentation.kit.theme.colorscheme.LavenderColorScheme
import app.katari.presentation.kit.theme.colorscheme.MidnightDuskColorScheme
import app.katari.presentation.kit.theme.colorscheme.MonochromeColorScheme
import app.katari.presentation.kit.theme.colorscheme.NordColorScheme
import app.katari.presentation.kit.theme.colorscheme.StrawberryColorScheme
import app.katari.presentation.kit.theme.colorscheme.TachiyomiColorScheme
import app.katari.presentation.kit.theme.colorscheme.TakoColorScheme
import app.katari.presentation.kit.theme.colorscheme.TealTurqoiseColorScheme
import app.katari.presentation.kit.theme.colorscheme.TidalWaveColorScheme
import app.katari.presentation.kit.theme.colorscheme.TokyoNightColorScheme
import app.katari.presentation.kit.theme.colorscheme.YinYangColorScheme
import app.katari.presentation.kit.theme.colorscheme.YotsubaColorScheme
import app.katari.presentation.kit.theme.colorscheme.rememberMonetColorScheme
import app.katari.presentation.kit.window.ProvideWindowLayout
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.domain.ui.model.TabletUiMode
import tachiyomi.presentation.core.util.collectAsState

@Composable
fun KatariTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val graph = rememberKatariThemeGraph()
    val uiPreferences = remember(graph) { graph.uiPreferences }
    val tabletUiMode by uiPreferences.tabletUiMode.collectAsState()
    val userTheme = currentUserTheme(uiPreferences)
    val fontFamily = rememberInterfaceFontFamily()
    val typography = remember(fontFamily) { katariTypography(fontFamily) }
    ProvideWindowLayout(wideLayoutsAllowed = tabletUiMode != TabletUiMode.NEVER) {
        BaseKatariTheme(
            appTheme = appTheme ?: userTheme.appTheme,
            isAmoled = amoled ?: userTheme.isAmoled,
            isDark = userTheme.isDark,
            typography = typography,
            content = content,
        )
    }
}

@Composable
fun KatariPreviewTheme(
    appTheme: AppTheme = AppTheme.DEFAULT,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseKatariTheme(appTheme, isAmoled, isSystemInDarkTheme(), DefaultTypography, content)

private val DefaultTypography = katariTypography(fontFamily = null)

@Composable
private fun BaseKatariTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    isDark: Boolean,
    typography: Typography,
    content: @Composable () -> Unit,
) {
    val monetColorScheme = rememberMonetColorScheme()
    val colorScheme = remember(appTheme, isDark, isAmoled, monetColorScheme) {
        getThemeColorScheme(
            monetColorScheme = monetColorScheme,
            appTheme = appTheme,
            isDark = isDark,
            isAmoled = isAmoled,
        )
    }
    val colors = remember(colorScheme, isDark) { KatariColors(colorScheme, isDark) }
    MaterialExpressiveTheme(colorScheme = colorScheme, typography = typography, shapes = KatariShapes.material) {
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

    /** Colours for drawing charts, the one part of the colour scheme drawing code outside the kit reads directly. */
    val chart: ChartColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKatariColors.current.chart

    /** Colours for marks drawn over a page or image, such as an outlined area or numbered text regions. */
    val markup: MarkupColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKatariColors.current.markup

    val spacing: KatariSpacing get() = KatariSpacing

    val sizes: KatariSizes get() = KatariSizes

    val shapes: KatariShapes
        @Composable
        @ReadOnlyComposable
        get() = KatariShapes.current()
}

private fun getThemeColorScheme(
    monetColorScheme: () -> BaseColorScheme,
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = if (appTheme == AppTheme.MONET) {
        monetColorScheme()
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
