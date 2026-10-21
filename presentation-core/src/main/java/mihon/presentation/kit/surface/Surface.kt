package mihon.presentation.kit.surface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import mihon.presentation.kit.theme.KatariColors
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import tachiyomi.presentation.core.components.material.Surface as ForkedSurface

/** What a surface is filled with; each container sets the matching content colour for what it holds. */
enum class SurfaceContainer {
    Background,
    Surface,
    Lowest,
    Low,
    Default,
    High,
    Highest,
    Primary,
    Secondary,
    Tertiary,
    PrimaryContainer,
    SecondaryContainer,
    TertiaryContainer,
    ErrorContainer,
    SuccessContainer,
    WarningContainer,
    Inverse,
    Selected,

    /** The shade over video and pages, with media content colours. */
    MediaScrim,

    /** The darkest media shade, for a full-screen layer whose text must read over whatever the video shows. */
    MediaShade,

    /** Behind full-screen media, such as a video or a page, with media content colours. */
    Media,

    /** The shade behind a modal surface, such as a drawer, dimming what it covers. */
    Scrim,
    Chrome,

    /** No fill; content keeps the inherited colour. */
    None,
}

/**
 * A container with a fill, a [shape] from KatariTheme.shapes and an optional outline. Clickable when [onClick] is set.
 */
@Composable
fun Surface(
    modifier: Modifier = Modifier,
    container: SurfaceContainer = SurfaceContainer.Surface,
    shape: Shape = KatariTheme.shapes.none,
    outlined: Boolean = false,
    shadow: Boolean = false,
    content: @Composable () -> Unit,
) {
    val (fill, contentColor) = container.colors(KatariTheme.colors)
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = shape,
        color = fill,
        contentColor = contentColor,
        shadowElevation = if (shadow) KatariTheme.sizes.shadow else KatariTheme.sizes.none,
        border = if (outlined) BorderStroke(KatariTheme.sizes.border, KatariTheme.colors.outlineVariant) else null,
        content = content,
    )
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: SurfaceContainer = SurfaceContainer.Surface,
    shape: Shape = KatariTheme.shapes.none,
    outlined: Boolean = false,
    shadow: Boolean = false,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable () -> Unit,
) {
    val (fill, contentColor) = container.colors(KatariTheme.colors)
    ForkedSurface(
        onClick = onClick,
        modifier = modifier,
        onLongClick = onLongClick,
        enabled = enabled,
        shape = shape,
        color = fill,
        contentColor = contentColor,
        shadowElevation = if (shadow) KatariTheme.sizes.shadow else KatariTheme.sizes.none,
        border = if (outlined) BorderStroke(KatariTheme.sizes.border, KatariTheme.colors.outlineVariant) else null,
        onClickLabel = onClickLabel,
        content = content,
    )
}

@Composable
@ReadOnlyComposable
internal fun SurfaceContainer.colors(colors: KatariColors): Pair<Color, Color> = when (this) {
    SurfaceContainer.Background -> colors.background to colors.onBackground
    SurfaceContainer.Surface -> colors.surface to colors.onSurface
    SurfaceContainer.Lowest -> colors.surfaceContainerLowest to colors.onSurface
    SurfaceContainer.Low -> colors.surfaceContainerLow to colors.onSurface
    SurfaceContainer.Default -> colors.surfaceContainer to colors.onSurface
    SurfaceContainer.High -> colors.surfaceContainerHigh to colors.onSurface
    SurfaceContainer.Highest -> colors.surfaceContainerHighest to colors.onSurface
    SurfaceContainer.Primary -> colors.primary to colors.onPrimary
    SurfaceContainer.Secondary -> colors.secondary to colors.onSecondary
    SurfaceContainer.Tertiary -> colors.tertiary to colors.onTertiary
    SurfaceContainer.PrimaryContainer -> colors.primaryContainer to colors.onPrimaryContainer
    SurfaceContainer.SecondaryContainer -> colors.secondaryContainer to colors.onSecondaryContainer
    SurfaceContainer.TertiaryContainer -> colors.tertiaryContainer to colors.onTertiaryContainer
    SurfaceContainer.ErrorContainer -> colors.errorContainer to colors.onErrorContainer
    SurfaceContainer.SuccessContainer -> colors.successContainer to colors.onSuccessContainer
    SurfaceContainer.WarningContainer -> colors.warningContainer to colors.onWarningContainer
    SurfaceContainer.Inverse -> colors.inverseSurface to colors.inverseOnSurface
    SurfaceContainer.Selected -> colors.selected to colors.onSecondaryContainer
    SurfaceContainer.MediaScrim -> colors.mediaScrim to colors.onMedia
    SurfaceContainer.MediaShade -> colors.mediaScrimEdge to colors.onMedia
    SurfaceContainer.Media -> colors.media to colors.onMedia
    SurfaceContainer.Scrim -> colors.scrim to colors.onMedia
    SurfaceContainer.Chrome -> colors.chrome to colors.onSurface
    SurfaceContainer.None -> Color.Transparent to LocalContentColor.current
}

/**
 * Fills this element with [container]'s colour, for a plain area such as the scrim behind the navigation bar. Its
 * content keeps the inherited content colour; use [Surface] for content that should take the container's.
 */
@Composable
fun Modifier.background(container: SurfaceContainer): Modifier = background(container.colors(KatariTheme.colors).first)

/** This container's fill as an Android colour, for a platform view standing in for it, such as a video's shutter. */
@Composable
@ReadOnlyComposable
fun SurfaceContainer.fillArgb(): Int = colors(KatariTheme.colors).first.toArgb()

/** Whether this container's fill is light, so what the system draws over it, such as status bar icons, must be dark. */
@Composable
@ReadOnlyComposable
fun SurfaceContainer.isLight(): Boolean = colors(KatariTheme.colors).first.luminance() > LIGHT_LUMINANCE

private const val LIGHT_LUMINANCE = 0.5f

/**
 * Gives [content] the content colour of [container] without drawing its fill, for content over something that stands
 * in for it, such as a window's root over its background or a header over the artwork behind it.
 */
@Composable
fun ContentOn(container: SurfaceContainer, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides container.colors(KatariTheme.colors).second, content = content)
}
