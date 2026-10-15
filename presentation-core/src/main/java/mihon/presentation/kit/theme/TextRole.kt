package mihon.presentation.kit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

/** What a piece of text is for. The role fixes its size, weight and default colour. */
enum class TextRole {
    /** App bar titles. */
    ScreenTitle,

    /** Titles of sheets and dialogs. */
    SheetTitle,

    /** Headings inside cards and sheets. */
    SectionTitle,

    /** Headers above a group of rows. */
    GroupLabel,

    /** Titles of list rows on screens. */
    ItemTitle,

    /** Paragraphs, descriptions and titles of compact rows. */
    Body,

    /** Row subtitles, hints and other secondary text. */
    Supporting,

    /** Buttons, tabs and chips. */
    Label,

    /** Pills, badges and counters. */
    Badge,

    /** Titles of entries laid under or over their covers in grids. */
    Caption,

    /** Large numbers in statistics. */
    Metric,

    /** Oversized text such as the empty-screen face. */
    Display,

    /** The one largest figure of a page, above [Display], such as the headline number of a shared story page. */
    Hero,

    /** Machine text such as logs, schemas and search syntax, in a monospace face. */
    Code,
}

internal fun TextRole.style(typography: Typography): TextStyle = when (this) {
    TextRole.ScreenTitle -> typography.titleLarge
    TextRole.SheetTitle -> typography.headlineSmall
    TextRole.SectionTitle -> typography.titleMedium
    TextRole.GroupLabel -> typography.titleSmall
    TextRole.ItemTitle -> typography.bodyLarge
    TextRole.Body -> typography.bodyMedium
    TextRole.Supporting -> typography.bodySmall
    TextRole.Label -> typography.labelLarge
    TextRole.Badge -> typography.labelSmall
    TextRole.Caption -> typography.labelMedium
    TextRole.Metric -> typography.headlineMedium
    TextRole.Display -> typography.displayMedium
    TextRole.Hero -> typography.displayLarge
    TextRole.Code -> typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
}

/** The colour a role uses unless the caller asks for a tone; [Color.Unspecified] inherits the content colour. */
internal fun TextRole.defaultColor(colors: KatariColors): Color = when (this) {
    TextRole.GroupLabel -> colors.primary
    TextRole.Supporting -> colors.onSurfaceVariant
    else -> Color.Unspecified
}
