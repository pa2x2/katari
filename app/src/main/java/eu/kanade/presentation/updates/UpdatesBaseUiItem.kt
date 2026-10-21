package eu.kanade.presentation.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.EntryCover as EntryCoverData

/**
 * An update's row: the entry's cover and title over a line of details from [subtitle], with an optional [trailing]
 * control. Read updates are dimmed; [subtitle] receives the tone its text takes for that.
 *
 * A [nested] row lists one update of an expanded group under the group's row: it leaves the cover and title to the
 * group and starts [subtitle] where the group's title starts, so the group reads as one entry.
 */
@Composable
fun UpdatesBaseUiItem(
    title: String,
    memberTitle: String?,
    coverData: EntryCoverData,
    selected: Boolean,
    read: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClickCover: (() -> Unit)? = null,
    subtitle: @Composable RowScope.(tone: ContentTone) -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    nested: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    val textTone = if (read) ContentTone.Disabled else ContentTone.Inherit

    Row(
        modifier = modifier
            .then(if (selected) Modifier.background(KatariTheme.colors.selected) else Modifier)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    onLongClick()
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                },
            )
            .padding(
                start = if (nested) NestedInset else KatariTheme.spacing.m,
                end = KatariTheme.spacing.m,
                top = if (nested) KatariTheme.spacing.xs else KatariTheme.spacing.s,
                bottom = if (nested) KatariTheme.spacing.xs else KatariTheme.spacing.s,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!nested) {
            EntryCover.Book(
                modifier = Modifier.size(LibraryMetrics.rowCover),
                data = coverData,
                shape = KatariTheme.shapes.coverThumbnail,
                onClick = onClickCover,
            )
        }

        Column(
            modifier = Modifier
                .then(
                    if (nested) {
                        Modifier.padding(end = KatariTheme.spacing.l)
                    } else {
                        Modifier.padding(horizontal = KatariTheme.spacing.l)
                    },
                )
                .weight(1f),
        ) {
            if (!nested) {
                Text(
                    text = title,
                    role = TextRole.ItemTitle,
                    tone = textTone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            memberTitle?.let {
                Text(
                    text = it,
                    role = TextRole.Supporting,
                    tone = textTone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                subtitle(textTone)
            }
        }

        trailing?.invoke()
    }
}

/** Where a group's title starts: the row's own inset, the cover and the gap after it. */
private val NestedInset
    @Composable get() = KatariTheme.spacing.m + LibraryMetrics.rowCover.width + KatariTheme.spacing.l
