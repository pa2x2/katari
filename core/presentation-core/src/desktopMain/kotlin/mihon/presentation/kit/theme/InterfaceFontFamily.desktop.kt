package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.SystemFont
import org.jetbrains.skia.FontMgr

// Compose maps Material's default family to a fixed list of fonts on Linux, not to the desktop's choice. The desktop's
// family is loaded from the system by name in every weight and slant, so the font manager picks each style's own face.
// A family the font manager doesn't have keeps the default.
@Composable
internal actual fun rememberInterfaceFontFamily(): FontFamily? {
    val font = rememberKatariThemeGraph().systemInterfaceFont
    return remember(font) {
        font.familyName
            ?.takeIf { name -> FontMgr.default.matchFamily(name).use { it.count() > 0 } }
            ?.let(::systemFontFamily)
    }
}

// Loading an installed font by name has no stable API on desktop yet
@OptIn(ExperimentalTextApi::class)
private fun systemFontFamily(name: String): FontFamily = FontFamily(
    WEIGHTS.flatMap { weight ->
        listOf(FontStyle.Normal, FontStyle.Italic).map { style -> SystemFont(name, weight, style) }
    },
)

private val WEIGHTS = listOf(
    FontWeight.Thin,
    FontWeight.ExtraLight,
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold,
    FontWeight.Black,
)
