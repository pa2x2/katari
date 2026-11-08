package mihon.presentation.kit.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Colour of text and icons relative to where they sit. [Inherit] takes the colour of the enclosing container. */
enum class ContentTone {
    Inherit,
    Primary,

    /** Secondary text and icons: onSurfaceVariant. */
    Secondary,
    Error,
    Success,
    Warning,

    /** A filter or mode that is switched on. */
    Active,
    OnMedia,
    OnMediaVariant,
    Disabled,

    /** The image's own colours, for logos and artwork. Text treats it as [Inherit]. */
    Original,
}

@Composable
@ReadOnlyComposable
internal fun ContentTone.color(fallback: Color = Color.Unspecified): Color {
    val colors = KatariTheme.colors
    return when (this) {
        ContentTone.Inherit -> fallback
        ContentTone.Primary -> colors.primary
        ContentTone.Secondary -> colors.onSurfaceVariant
        ContentTone.Error -> colors.error
        ContentTone.Success -> colors.success
        ContentTone.Warning -> colors.warning
        ContentTone.Active -> colors.active
        ContentTone.OnMedia -> colors.onMedia
        ContentTone.OnMediaVariant -> colors.onMediaVariant
        ContentTone.Disabled -> colors.disabledContent
        ContentTone.Original -> Color.Unspecified
    }
}

/** Colour that text and icons inherit; kit containers set it for their content. */
val LocalContentColor: ProvidableCompositionLocal<Color> = androidx.compose.material3.LocalContentColor
