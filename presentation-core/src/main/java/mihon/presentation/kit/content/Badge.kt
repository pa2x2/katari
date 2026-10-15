package mihon.presentation.kit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import mihon.presentation.kit.theme.KatariTheme

/** Badges laid side by side share one rounded outline, as on library covers. */
@Composable
fun BadgeGroup(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.clip(KatariTheme.shapes.extraSmall),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxs),
    ) {
        content()
    }
}

/** What a badge stands for; each tone is a filled container role with its matching content colour. */
enum class BadgeTone {
    Primary,
    Secondary,
    Tertiary,
    Error,

    /** Quiet information beside a stronger badge, such as a duplicate's reasons next to its score. */
    Muted,

    /** Counts and states that should not draw the eye, such as queued work or downloads. */
    Neutral,
}

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Secondary,
    icon: ImageVector? = null,
) {
    BadgeContent(text = text, icon = icon, modifier = modifier, tone = tone)
}

@Composable
fun Badge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Secondary,
) {
    BadgeContent(text = "", icon = icon, modifier = modifier, tone = tone)
}

/** A small count or dot pinned to a navigation item or icon; [count] null shows only the dot. */
@Composable
fun NotificationBadge(modifier: Modifier = Modifier, count: Int? = null) {
    androidx.compose.material3.Badge(modifier = modifier) {
        if (count != null) Text(count.toString())
    }
}

/** Pins [badge], such as a [NotificationBadge], to the top end corner of [content], usually an icon. */
@Composable
fun BadgedBox(
    badge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.BadgedBox(badge = { badge() }, modifier = modifier) { content() }
}

/** Badge text, optionally led by an icon sized to the text so both badge forms share one height. */
@Composable
private fun BadgeContent(
    text: String,
    icon: ImageVector?,
    modifier: Modifier,
    tone: BadgeTone,
) {
    val colors = KatariTheme.colors
    val (color, contentColor) = when (tone) {
        BadgeTone.Primary -> colors.primary to colors.onPrimary
        BadgeTone.Secondary -> colors.secondary to colors.onSecondary
        BadgeTone.Tertiary -> colors.tertiary to colors.onTertiary
        BadgeTone.Error -> colors.error to colors.onError
        BadgeTone.Muted -> colors.secondaryContainer to colors.onSecondaryContainer
        BadgeTone.Neutral -> colors.surfaceContainerHighest to colors.onSurface
    }
    val iconContentPlaceholder = "[icon]"
    val annotatedText = buildAnnotatedString {
        if (icon != null) appendInlineContent(iconContentPlaceholder)
        append(text)
    }
    val inlineContent = if (icon == null) {
        emptyMap()
    } else {
        mapOf(
            iconContentPlaceholder to InlineTextContent(
                Placeholder(
                    width = MaterialTheme.typography.bodySmall.fontSize,
                    height = MaterialTheme.typography.bodySmall.fontSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Icon(
                    imageVector = icon,
                    tint = contentColor,
                    contentDescription = null,
                )
            },
        )
    }

    Text(
        text = annotatedText,
        inlineContent = inlineContent,
        modifier = modifier
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        color = contentColor,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}
