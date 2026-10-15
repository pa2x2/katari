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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.LocalContentColor
import mihon.presentation.kit.theme.TextRole

/**
 * The base of every row: optional [icon] or [leading] control, a title with an optional subtitle, and an optional
 * [trailing] control. Navigation rows are this row with [onClick]; they never show a chevron.
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
) {
    RowFrame(
        modifier = modifier,
        enabled = enabled,
        role = role,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        when {
            leading != null -> leading()
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
    enabled: Boolean,
    role: Role?,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalRowDensity.current
    val clickable = onClick != null || onLongClick != null
    Row(
        modifier = modifier
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
            .padding(horizontal = LocalRowInset.current, vertical = density.verticalPadding),
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
