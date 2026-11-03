package mihon.presentation.kit.theme.colorscheme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.ktx.DynamicScheme
import com.materialkolor.toColorScheme

/** Makes the scheme of the Dynamic theme from the platform's colours; only called when that theme is chosen. */
@Composable
internal expect fun rememberMonetColorScheme(): () -> BaseColorScheme

internal class MonetCompatColorScheme(seed: Color) : BaseColorScheme() {
    override val lightScheme = generateColorSchemeFromSeed(seed = seed, dark = false)
    override val darkScheme = generateColorSchemeFromSeed(seed = seed, dark = true)

    companion object {
        fun generateColorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
            return DynamicScheme(
                seedColor = seed,
                isDark = dark,
                specVersion = ColorSpec.SpecVersion.SPEC_2025,
                style = PaletteStyle.Expressive,
            )
                .toColorScheme(isAmoled = false)
        }
    }
}
