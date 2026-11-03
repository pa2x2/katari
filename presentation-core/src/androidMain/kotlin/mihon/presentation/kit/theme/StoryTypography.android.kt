package mihon.presentation.kit.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import java.io.File

// Newer Android versions ship Roboto Flex as a system font but don't expose its width axis by family name, so it's
// read from its file; older ones fall back to the default font.
internal actual fun robotoFlex(weight: Int, width: Float): Font? {
    val file = File(ROBOTO_FLEX_PATH).takeIf { it.canRead() } ?: return null
    return Font(
        file = file,
        weight = FontWeight(weight),
        variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
    )
}

private const val ROBOTO_FLEX_PATH = "/system/fonts/RobotoFlex-Regular.ttf"
