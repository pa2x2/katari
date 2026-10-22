package mihon.presentation.kit.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.offset
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LogoSize
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.surface.background
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole

/**
 * The base of every row: optional [icon] or [leading] control, a title with an optional subtitle, and an optional
 * [trailing] control. Navigation rows are this row with [onClick]; they never show a chevron.
 *
 * A [nested] row belongs to the logo-led row above it, such as a source's feeds under the source: it starts where that
 * row's text does.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    titleSuffix: (@Composable () -> Unit)? = null,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
    role: Role? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
    nested: Boolean = false,
) {
    RowFrame(
        modifier = modifier,
        indent = if (nested) LogoSize.Row.side(KatariTheme.sizes) + KatariTheme.spacing.l else KatariTheme.sizes.none,
        enabled = enabled,
        role = role,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        when {
            leading != null -> leading()
            icon != null && LocalInRowGroup.current -> RowIconTile(icon)
            icon != null -> Icon(icon, contentDescription = null, tone = ContentTone.Secondary)
        }
        Column(modifier = Modifier.weight(1f)) {
            RowTitle(title, titleSuffix)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, role = TextRole.Supporting, maxLines = subtitleMaxLines)
            }
            below?.invoke()
        }
        trailing?.invoke()
    }
}

@Composable
internal fun RowFrame(
    modifier: Modifier,
    indent: Dp = KatariTheme.sizes.none,
    enabled: Boolean,
    role: Role?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalRowDensity.current
    val clickable = onClick != null || onLongClick != null
    val inDialog = LocalInDialog.current
    val bleed = if (inDialog) KatariTheme.spacing.m else KatariTheme.sizes.none
    Row(
        modifier = Modifier
            .then(
                if (inDialog) {
                    Modifier
                        .padding(vertical = KatariTheme.spacing.xxs / 2)
                        .bleed(bleed)
                        .clip(KatariTheme.shapes.medium)
                } else {
                    Modifier
                },
            )
            .then(modifier)
            .then(
                if (clickable) {
                    Modifier.combinedClickable(
                        enabled = enabled,
                        role = role,
                        onLongClick = onLongClick,
                        onClick = { onClick?.invoke() },
                    )
                } else {
                    Modifier
                },
            )
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current + bleed, vertical = density.verticalPadding)
            .padding(start = indent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
    ) {
        if (enabled) {
            content()
        } else {
            CompositionLocalProvider(LocalContentColor provides KatariTheme.colors.disabledContent) { content() }
        }
    }
}

@Composable
internal fun RowTitle(title: String, suffix: (@Composable () -> Unit)? = null) {
    val role = LocalRowDensity.current.titleRole
    if (suffix == null) {
        Text(title, role = role)
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, role = role)
            suffix()
        }
    }
}

/** Fills a choice row while it is chosen, when it sits in a dialog; elsewhere its row group or control shows that. */
@Composable
internal fun Modifier.dialogSelectionFill(selected: Boolean): Modifier =
    if (selected && LocalInDialog.current) background(SurfaceContainer.Selected) else this

// Widens the element by [horizontal] on each side without taking more room in its parent.
private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(constraints.offset(horizontal = extra))
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}
