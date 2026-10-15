package mihon.presentation.kit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.io.File

/*
 * The type scale of a story page. Pages are drawn at a fixed size with a font scale of 1, so these sizes are exact on
 * the shared image. Each text role maps to the slot it uses everywhere else, so page code picks roles as usual:
 * Hero and Display for the page's big figure, Metric for headlines, SheetTitle for figures beside text, Label for the
 * line naming what a page is about, and the body roles for sentences, rows and footnotes.
 */
internal fun storyTypography(base: Typography): Typography {
    val heavy = condensed(width = 30f, weight = 900)
    return base.copy(
        displayLarge = TextStyle(
            fontFamily = heavy,
            fontWeight = FontWeight.Black,
            fontSize = 128.sp,
            lineHeight = 116.sp,
        ),
        displayMedium = TextStyle(
            fontFamily = heavy,
            fontWeight = FontWeight.Black,
            fontSize = 88.sp,
            lineHeight = 82.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = condensed(width = 40f, weight = 850),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 36.sp,
            lineHeight = 38.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = condensed(width = 60f, weight = 750),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 28.sp,
        ),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
        // Calendar day numbers: titleSmall marks the days of a run, bodyLarge every other day.
        titleSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 18.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.1.em,
        ),
        bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 25.sp),
        bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 17.sp),
        labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 13.sp),
    )
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
