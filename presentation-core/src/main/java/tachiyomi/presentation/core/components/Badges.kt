package tachiyomi.presentation.core.components

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

@Composable
fun BadgeGroup(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraSmall,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.clip(shape),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

@Composable
fun Badge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    textColor: Color = MaterialTheme.colorScheme.onSecondary,
    shape: Shape = RectangleShape,
    icon: ImageVector? = null,
) {
    BadgeContent(
        text = text,
        icon = icon,
        modifier = modifier,
        color = color,
        contentColor = textColor,
        shape = shape,
    )
}

@Composable
fun Badge(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    iconColor: Color = MaterialTheme.colorScheme.onSecondary,
    shape: Shape = RectangleShape,
) {
    BadgeContent(
        text = "",
        icon = imageVector,
        modifier = modifier,
        color = color,
        contentColor = iconColor,
        shape = shape,
    )
}

/** Badge text, optionally led by an icon sized to the text so both badge forms share one height. */
@Composable
private fun BadgeContent(
    text: String,
    icon: ImageVector?,
    modifier: Modifier,
    color: Color,
    contentColor: Color,
    shape: Shape,
) {
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
            .clip(shape)
            .background(color)
            .padding(horizontal = 3.dp, vertical = 1.dp),
        color = contentColor,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        style = MaterialTheme.typography.bodySmall,
    )
}
