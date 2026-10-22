package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.list.CoverRow
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.domain.entry.model.EntryCover as EntryCoverModel

/**
 * An entry in a list display mode, set in a row group card by its list. Every source's cover is cropped into the row's
 * portrait thumbnail, as in Updates and History.
 */
@Composable
fun EntryListItem(
    coverData: EntryCoverModel,
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    badge: @Composable (RowScope.() -> Unit),
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    coverAlpha: Float = 1f,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    selectionMode: Boolean = false,
) {
    CoverRow(
        cover = {
            EntryCover.Book(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(coverAlpha),
                data = coverData,
                frame = CoverFrame.Thumbnail,
            )
        },
        modifier = modifier,
        selected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = {
            BadgeGroup(content = badge)
            coverTrailingAction(
                selectionMode = selectionMode,
                isSelected = isSelected,
                onClickContinueReading = onClickContinueReading,
                continueReadingProgress = continueReadingProgress,
                continueReadingContentDescription = continueReadingContentDescription,
                large = false,
                onCover = false,
            )?.invoke(Modifier)
        },
    ) {
        Text(
            text = title,
            role = TextRole.ItemTitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
