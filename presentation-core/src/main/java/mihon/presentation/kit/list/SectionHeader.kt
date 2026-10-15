package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * The header above a group of rows, on screens and in sheets alike. [trailing] holds a small marker such as a chip,
 * right after the text; [actions] sit at the header's end and act on the whole group, such as "Update all" or sort
 * toggles.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
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
            Text(text, role = TextRole.GroupLabel)
            trailing?.invoke()
        }
        actions?.invoke(this)
    }
}
