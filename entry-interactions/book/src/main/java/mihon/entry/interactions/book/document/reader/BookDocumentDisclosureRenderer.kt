package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader

/** Expandable recursive semantic disclosure renderer. */
@Composable
internal fun BookDocumentDisclosureRenderer(
    content: BookDocumentBlockContent.Disclosure,
    sectionKey: String,
    resourceLoader: BookPublicationResourceLoader?,
    onAnchorClick: (String) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onReaderTap: () -> Unit,
    preserveTerminalSpacing: Boolean,
) {
    var expanded by remember(content) { mutableStateOf(content.initiallyExpanded) }
    Text(
        text = (if (expanded) "▾ " else "▸ ") + content.summary.text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
    )
    if (expanded) {
        content.body.blocks.forEachIndexed { index, nested ->
            BookDocumentBlockRenderer(
                block = nested,
                owningContent = content.body,
                sectionKey = sectionKey,
                resourceLoader = resourceLoader,
                onAnchorClick = onAnchorClick,
                onExternalLinkClick = onExternalLinkClick,
                onReaderTap = onReaderTap,
                preserveTerminalSpacing = index != content.body.blocks.lastIndex || preserveTerminalSpacing,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
