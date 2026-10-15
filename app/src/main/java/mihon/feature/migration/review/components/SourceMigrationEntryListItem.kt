package mihon.feature.migration.review.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * An entry of a migration, a source entry or a replacement, as its cover, title and [supportingText]. [inset] is the
 * row's side padding, smaller when the row sits in a panel that is itself inset, so covers line up down a card.
 * [dimmed] marks a member the migration leaves alone.
 */
@Composable
internal fun SourceMigrationEntryListItem(
    title: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    inset: Dp = KatariTheme.spacing.rowInset,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    supportingText: String? = null,
    supportingTrailing: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .fillMaxWidth()
            .padding(horizontal = inset, vertical = KatariTheme.spacing.s),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Book(
            data = thumbnailUrl,
            modifier = Modifier.height(KatariTheme.sizes.artworkLarge),
        )
        Column(modifier = Modifier.weight(1f)) {
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
        trailingContent?.invoke()
    }
}
