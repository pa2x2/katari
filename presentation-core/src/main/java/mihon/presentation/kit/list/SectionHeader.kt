package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * The header above a group of rows, on screens and in sheets alike. Screens reach it through [RowGroupHeader] or
 * [RowGroupSubheader], which place it against the card it names. [trailing] holds a small marker such as a chip, right
 * after the text; [actions] sit at the header's end and act on the whole group, such as "Update all" or sort toggles.
 * [icon] leads the text, such as an outcome of a report, and [tone] marks a group that reports a problem.
 */
@Composable
internal fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: ContentTone = ContentTone.Inherit,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    // The text falls back to the header colour of its role; the icon has no role to fall back on.
    val iconTone = if (tone == ContentTone.Inherit) ContentTone.Primary else tone
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = LocalRowInset.current, end = LocalRowInset.current, top = spacing.l, bottom = spacing.s),
        horizontalArrangement = Arrangement.spacedBy(spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tone = iconTone)
            }
            Text(text, role = TextRole.GroupLabel, tone = tone)
            trailing?.invoke()
        }
        actions?.invoke(this)
    }
}
