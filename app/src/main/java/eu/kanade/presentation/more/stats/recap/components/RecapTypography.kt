package eu.kanade.presentation.more.stats.recap.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * Text styles of recap pages. Pages are drawn at a fixed size with a font scale of 1, so these sizes are exact on the
 * shared image.
 */
internal object RecapTypography {
    val Huge =
        TextStyle(
            fontFamily = condensed(30f, 900),
            fontWeight = FontWeight.Black,
            fontSize = 128.sp,
            lineHeight = 116.sp,
        )
    val Display =
        TextStyle(fontFamily = condensed(30f, 900), fontWeight = FontWeight.Black, fontSize = 88.sp, lineHeight = 82.sp)
    val Headline =
        TextStyle(
            fontFamily = condensed(40f, 850),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            lineHeight = 38.sp,
        )
    val Title =
        TextStyle(fontFamily = condensed(60f, 750), fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 28.sp)
    val Rank =
        TextStyle(fontFamily = condensed(30f, 900), fontWeight = FontWeight.Black, fontSize = 36.sp, lineHeight = 36.sp)
    val Kicker =
        TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.1.em)
    val Body = TextStyle(fontSize = 18.sp, lineHeight = 25.sp)
    val RowTitle = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)
    val Small = TextStyle(fontSize = 13.sp, lineHeight = 17.sp)
    val Tiny = TextStyle(fontSize = 11.sp, lineHeight = 13.sp)
}

/**
 * Roboto Flex at a narrow width, for the large figures. Newer Android versions ship it as a system font but don't
 * expose its width axis by family name, so it's read from its file; older ones fall back to the default font.
 */
private fun condensed(width: Float, weight: Int): FontFamily {
    val file = File(ROBOTO_FLEX_PATH).takeIf { it.canRead() } ?: return FontFamily.Default
    return FontFamily(
        Font(
            file = file,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight), FontVariation.width(width)),
        ),
    )
}

private const val ROBOTO_FLEX_PATH = "/system/fonts/RobotoFlex-Regular.ttf"
