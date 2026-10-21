package eu.kanade.presentation.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
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

    CoverRow(
        cover = if (nested) {
            null
        } else {
            {
                EntryCover.Book(
                    modifier = Modifier.fillMaxSize(),
                    data = coverData,
                    shape = KatariTheme.shapes.coverThumbnail,
                    onClick = onClickCover,
                )
            }
        },
        modifier = modifier,
        selected = selected,
        onClick = onClick,
        onLongClick = {
            onLongClick()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        },
        trailing = trailing,
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
}
