package mihon.presentation.kit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

/**
 * The Material type scale with Expressive's emphasized title. App bars and other Material components read titleLarge
 * straight from the theme, so its weight is set here; the other emphasized roles only change in [TextRole.style],
 * where Material's own text fields, tabs and pickers don't see them.
 */
internal val KatariTypography: Typography = Typography().run {
    copy(titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium))
}
