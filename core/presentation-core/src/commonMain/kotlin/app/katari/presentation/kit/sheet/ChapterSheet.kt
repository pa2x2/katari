package app.katari.presentation.kit.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.MouseSelectableText
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.list.ChapterDetails
import app.katari.presentation.kit.list.ChapterPreview
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole

/**
 * What a source says about one chapter or episode, opened from its row's still or info button: the [preview], its
 * [title] and details line as in the row, the whole [summary], and [action], such as Play, at the bottom. The still
 * goes across the top, or beside the text in a sheet wide enough for two columns.
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
        val text = @Composable { textModifier: Modifier ->
            ChapterText(title, kind, date, duration, scanlator, summary, textModifier)
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.rowInset, end = spacing.rowInset, top = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.m),
            ) {
                when {
                    preview == null -> text(Modifier)
                    LocalSheetColumns.current > 1 -> Row(horizontalArrangement = Arrangement.spacedBy(spacing.l)) {
                        ChapterPreview(preview = preview, onClick = null, modifier = Modifier.weight(1f))
                        text(Modifier.weight(1f))
                    }
                    else -> {
                        ChapterPreview(preview = preview, onClick = null, modifier = Modifier.fillMaxWidth())
                        text(Modifier)
                    }
                }
            }
            SheetActions(confirm = action)
        }
    }
}

/** The chapter's title, ending with the sheet's close button, over its details line and summary. */
@Composable
private fun ChapterText(
    title: String,
    kind: String?,
    date: String?,
    duration: String?,
    scanlator: String?,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.m)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, role = TextRole.SheetTitle, modifier = Modifier.weight(1f))
                SheetCloseButton()
            }
            ChapterDetails(
                read = false,
                kind = kind,
                date = date,
                duration = duration,
                progress = null,
                scanlator = scanlator,
            )
        }
        if (summary != null) {
            MouseSelectableText { Text(summary, role = TextRole.Body, tone = ContentTone.Secondary) }
        }
    }
}
