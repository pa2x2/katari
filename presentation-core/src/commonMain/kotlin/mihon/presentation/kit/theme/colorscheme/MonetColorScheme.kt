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
        // Tonal Spot is the style Android's own Dynamic theme defaults to: near-neutral surfaces and secondary colours
        // close to the seed's hue. Louder styles such as Expressive tint every surface and rotate the secondary and
        // tertiary hues, so the app stops looking like the system it was seeded from.
        // The 2025 spec darkens dark surfaces well below the greys desktops use, so this uses the 2021 spec.
        fun generateColorSchemeFromSeed(seed: Color, dark: Boolean): ColorScheme {
            return DynamicScheme(
                seedColor = seed,
                isDark = dark,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
                style = PaletteStyle.TonalSpot,
            )
                .toColorScheme(isAmoled = false)
        }
    }
}
