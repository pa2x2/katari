package mihon.feature.migration.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * An entry of a migration, a source entry or a replacement, as its cover, title and [supportingText]. [dimmed] marks a
 * member the migration leaves alone.
 */
@Composable
internal fun SourceMigrationEntryListItem(
    title: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    supportingText: String? = null,
    supportingTrailing: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    CoverRow(
        cover = {
            EntryCover.Book(
                data = thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                frame = CoverFrame.Thumbnail,
            )
        },
        modifier = modifier,
        onClick = onClick?.takeIf { enabled },
        trailing = trailingContent,
    ) {
        Text(
            text = title,
            role = TextRole.ItemTitle,
            tone = if (dimmed) ContentTone.Secondary else ContentTone.Inherit,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (supportingText != null || supportingTrailing != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = supportingText.orEmpty(),
                    modifier = Modifier.weight(1f),
                    role = TextRole.Body,
                    tone = ContentTone.Secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                supportingTrailing?.invoke()
            }
        }
    }
}
