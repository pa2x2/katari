package eu.kanade.presentation.library.update.report.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover

@Composable
internal fun ReportRow(
    entry: Entry?,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .height(LibraryMetrics.listRowHeight)
            .padding(horizontal = KatariTheme.spacing.contentInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (entry != null) {
            EntryCover.Square(
                data = entry.asEntryCover(),
                modifier = Modifier
                    .padding(vertical = KatariTheme.spacing.s)
                    .fillMaxHeight(),
            )
        }
        Column(
            modifier = Modifier
                .padding(horizontal = KatariTheme.spacing.l)
                .weight(1f),
        ) {
            Text(
                text = title,
                role = TextRole.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    role = TextRole.Supporting,
                    tone = if (isError) ContentTone.Error else ContentTone.Inherit,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}
