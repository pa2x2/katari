package mihon.presentation.kit.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.list.ChapterDetails
import mihon.presentation.kit.list.ChapterPreview
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * What a source says about one chapter or episode, opened from its row's still: the [preview] across the top, its
 * [title] and details line as in the row, the whole [summary], and [action], such as Play, at the bottom.
 *
 * @param preview draws the still with the modifier it is given, which sizes and clips it.
 */
@Composable
fun ChapterSheet(
    title: String,
    onDismissRequest: () -> Unit,
    action: DialogAction,
    modifier: Modifier = Modifier,
    kind: String? = null,
    date: String? = null,
    duration: String? = null,
    scanlator: String? = null,
    summary: String? = null,
    preview: (@Composable (Modifier) -> Unit)? = null,
) {
    val spacing = KatariTheme.spacing
    Sheet(onDismissRequest = onDismissRequest, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.rowInset, end = spacing.rowInset, top = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                if (preview != null) {
                    ChapterPreview(preview = preview, onClick = null, modifier = Modifier.fillMaxWidth())
                }
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(title, role = TextRole.SheetTitle)
                    ChapterDetails(
                        read = false,
                        kind = kind,
                        date = date,
                        duration = duration,
                        progress = null,
                        scanlator = scanlator,
                    )
                }
                if (summary != null) Text(summary, role = TextRole.Body, tone = ContentTone.Secondary)
            }
            SheetActions(confirm = action)
        }
    }
}
