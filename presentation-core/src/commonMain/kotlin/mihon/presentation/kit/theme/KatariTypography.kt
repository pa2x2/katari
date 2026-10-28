package mihon.presentation.kit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The Material type scale with Expressive's emphasized title, set in [fontFamily] or Material's default family when
 * null. App bars and other Material components read titleLarge straight from the theme, so its weight is set here; the
 * other emphasized roles only change in [TextRole.style], where Material's own text fields, tabs and pickers don't see
 * them.
 */
internal fun katariTypography(fontFamily: FontFamily?): Typography {
    val typography = Typography().let { if (fontFamily != null) it.setIn(fontFamily) else it }
    return typography.copy(titleLarge = typography.titleLarge.copy(fontWeight = FontWeight.Medium))
}

private fun Typography.setIn(fontFamily: FontFamily): Typography = Typography(
    displayLarge = displayLarge.copy(fontFamily = fontFamily),
    displayMedium = displayMedium.copy(fontFamily = fontFamily),
    displaySmall = displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = titleLarge.copy(fontFamily = fontFamily),
    titleMedium = titleMedium.copy(fontFamily = fontFamily),
    titleSmall = titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = bodySmall.copy(fontFamily = fontFamily),
    labelLarge = labelLarge.copy(fontFamily = fontFamily),
    labelMedium = labelMedium.copy(fontFamily = fontFamily),
    labelSmall = labelSmall.copy(fontFamily = fontFamily),
    displayLargeEmphasized = displayLargeEmphasized.copy(fontFamily = fontFamily),
    displayMediumEmphasized = displayMediumEmphasized.copy(fontFamily = fontFamily),
    displaySmallEmphasized = displaySmallEmphasized.copy(fontFamily = fontFamily),
    headlineLargeEmphasized = headlineLargeEmphasized.copy(fontFamily = fontFamily),
    headlineMediumEmphasized = headlineMediumEmphasized.copy(fontFamily = fontFamily),
    headlineSmallEmphasized = headlineSmallEmphasized.copy(fontFamily = fontFamily),
    titleLargeEmphasized = titleLargeEmphasized.copy(fontFamily = fontFamily),
    titleMediumEmphasized = titleMediumEmphasized.copy(fontFamily = fontFamily),
    titleSmallEmphasized = titleSmallEmphasized.copy(fontFamily = fontFamily),
    bodyLargeEmphasized = bodyLargeEmphasized.copy(fontFamily = fontFamily),
    bodyMediumEmphasized = bodyMediumEmphasized.copy(fontFamily = fontFamily),
    bodySmallEmphasized = bodySmallEmphasized.copy(fontFamily = fontFamily),
    labelLargeEmphasized = labelLargeEmphasized.copy(fontFamily = fontFamily),
    labelMediumEmphasized = labelMediumEmphasized.copy(fontFamily = fontFamily),
    labelSmallEmphasized = labelSmallEmphasized.copy(fontFamily = fontFamily),
)
