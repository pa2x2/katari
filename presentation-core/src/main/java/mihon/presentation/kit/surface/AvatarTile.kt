package mihon.presentation.kit.surface

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor

/** Side of an [AvatarTile]. */
val AvatarTileSize: Dp = 118.dp

/** Where a compact [Avatar] sits, which sets its size and shape. */
enum class AvatarSize(internal val side: Dp) {
    /** Inside a line of text or a chip. */
    Inline(16.dp),

    /** In place of an icon, such as a navigation item's, so labels stay aligned with their neighbours. */
    Icon(24.dp),

    /** An app bar action. */
    Bar(30.dp),

    /** The leading slot of a list row or card. */
    Row(40.dp),
}

/**
 * A square avatar, such as a profile's, filled with a gradient picked from [seed] so the same seed keeps its colours in
 * every theme; [selected] rings it in the primary colour. [content] draws on top, such as a face or a lock badge, and
 * takes the on-primary colour as its content colour.
 */
@Composable
fun AvatarTile(
    seed: Long,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = KatariTheme.colors
    val shape = KatariTheme.shapes.medium
    val fill = remember(seed) { avatarFill(seed) }
    Box(
        modifier = modifier
            .size(AvatarTileSize)
            .clip(shape)
            .background(fill)
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.surface.copy(alpha = SHEEN_ALPHA),
                        Color.Transparent,
                        colors.media.copy(alpha = SHADE_ALPHA),
                    ),
                ),
            )
            .border(
                width = if (selected) KatariTheme.sizes.selectedBorder else KatariTheme.sizes.border,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.onPrimary.copy(alpha = CONTENT_ALPHA)) {
            content()
        }
    }
}

/**
 * The compact form of [AvatarTile] for bars, rows and chips: the same seeded gradient, round in text, icons and bars and a
 * rounded square in rows. [badge] marks a state over its corner, such as a lock. It is not clickable itself; its
 * container handles taps.
 */
@Composable
fun Avatar(
    seed: Long,
    size: AvatarSize,
    modifier: Modifier = Modifier,
    badge: ImageVector? = null,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val colors = KatariTheme.colors
    val shape = if (size == AvatarSize.Row) KatariTheme.shapes.small else KatariTheme.shapes.full
    val fill = remember(seed) { avatarFill(seed) }
    Box(modifier = modifier.size(size.side)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(fill)
                .border(width = KatariTheme.sizes.border, color = colors.outlineVariant, shape = shape),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.onPrimary.copy(alpha = CONTENT_ALPHA)) {
                content()
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = AvatarBadgeOverhang, y = AvatarBadgeOverhang),
                container = SurfaceContainer.Chrome,
                shape = KatariTheme.shapes.full,
            ) {
                Icon(
                    imageVector = badge,
                    contentDescription = null,
                    modifier = Modifier.padding(KatariTheme.spacing.xxs).size(AvatarBadgeIconSize),
                )
            }
        }
    }
}

/** How far an [Avatar]'s badge reaches past its edge, so it reads as on top rather than cut into the avatar. */
private val AvatarBadgeOverhang = 4.dp
private val AvatarBadgeIconSize = 12.dp

private fun avatarFill(seed: Long): Brush {
    val hue = ((seed % FULL_TURN) + FULL_TURN) % FULL_TURN
    return Brush.linearGradient(
        colors = listOf(
            Color.hsv(hue.toFloat(), START_SATURATION, START_VALUE),
            Color.hsv(((hue + HUE_STEP) % FULL_TURN).toFloat(), END_SATURATION, END_VALUE),
        ),
    )
}

private const val FULL_TURN = 360L
private const val HUE_STEP = 42L
private const val START_SATURATION = 0.54f
private const val START_VALUE = 0.88f
private const val END_SATURATION = 0.6f
private const val END_VALUE = 0.72f
private const val SHEEN_ALPHA = 0.06f
private const val SHADE_ALPHA = 0.16f
private const val CONTENT_ALPHA = 0.92f
