package eu.kanade.presentation.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .height(if (memberTitle != null) LibraryMetrics.listRowTallHeight else LibraryMetrics.listRowHeight)
            .padding(horizontal = KatariTheme.spacing.contentInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryCover.Square(
            modifier = Modifier
                .padding(vertical = KatariTheme.spacing.s)
                .fillMaxHeight(),
            data = coverData,
            shape = KatariTheme.shapes.coverThumbnail,
            onClick = onClickCover,
        )

        Column(
            modifier = Modifier
                .padding(horizontal = KatariTheme.spacing.l)
                .weight(1f),
        ) {
            Text(
                text = title,
                role = TextRole.Body,
                tone = textTone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

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
