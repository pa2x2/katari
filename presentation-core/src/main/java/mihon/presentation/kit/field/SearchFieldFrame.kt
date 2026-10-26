package mihon.presentation.kit.field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Search
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.Tooltip
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * The rounded box every search field is drawn in, in a toolbar or above a list: a search icon, the placeholder while
 * [query] is empty, and a clear button labelled [clearLabel] once it isn't. [counter], such as "1 of 2", sits before
 * the clear button.
 */
@Composable
internal fun SearchFieldFrame(
    query: String,
    placeholder: String,
    clearLabel: String,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    counter: String? = null,
    innerTextField: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .heightIn(min = SearchFieldHeight)
            .background(KatariTheme.colors.surfaceContainerHigh, KatariTheme.shapes.full)
            .padding(start = KatariTheme.spacing.l, end = KatariTheme.spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(MaterialSymbols.Rounded.Search, contentDescription = null, tone = ContentTone.Secondary)
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                BasicText(
                    text = placeholder,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = searchFieldTextStyle().copy(color = KatariTheme.colors.onSurfaceVariant),
                )
            }
            innerTextField()
        }
        if (counter != null) {
            Text(counter, role = TextRole.Supporting, tone = ContentTone.Secondary, maxLines = 1)
        }
        if (query.isNotEmpty()) {
            Tooltip(clearLabel) {
                IconButton(
                    icon = MaterialSymbols.Rounded.Close,
                    contentDescription = clearLabel,
                    onClick = onClear,
                    tone = ContentTone.Secondary,
                )
            }
        }
    }
}

@Composable
internal fun searchFieldTextStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(color = KatariTheme.colors.onSurface)

private val SearchFieldHeight = 48.dp
