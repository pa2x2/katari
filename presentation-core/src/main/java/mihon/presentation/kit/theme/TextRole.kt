package mihon.presentation.kit.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** What a piece of text is for. The role fixes its size, weight and default colour. */
enum class TextRole {
    /** The expanded title of a large app bar. */
    LargeTitle,

    /** App bar titles. */
    ScreenTitle,

    /** Titles of sheets and dialogs. */
    SheetTitle,

    /** The name of what a details page is about, such as an entry's title in its header. */
    DetailTitle,

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

    /** Small captions, such as banner notices and notes inside a card. */
    Caption,

    /** Titles of entries laid under or over their covers in grids. */
    CoverTitle,

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
    TextRole.LargeTitle -> typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold)
    TextRole.ScreenTitle -> typography.titleLarge
    TextRole.SheetTitle -> typography.headlineSmall
    TextRole.DetailTitle -> typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
    TextRole.SectionTitle -> typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    TextRole.GroupLabel -> typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
    TextRole.ItemTitle -> typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
    TextRole.Body -> typography.bodyMedium
    TextRole.Supporting -> typography.bodySmall
    TextRole.Label -> typography.labelLarge
    TextRole.Badge -> typography.labelSmall
    TextRole.Caption -> typography.labelMedium
    TextRole.CoverTitle -> typography.bodyMedium.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
    )
    TextRole.Metric -> typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
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
