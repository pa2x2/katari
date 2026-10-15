package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.domain.entry.model.EntryCover as EntryCoverModel

/**
 * Layout of list item.
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
    coverType: EntryCover = EntryCover.Square,
    coverContentScale: ContentScale = ContentScale.Crop,
    coverBackgroundColor: Color = Color.Transparent,
    coverAlpha: Float = 1f,
    coverReplacement: @Composable (BoxScope.() -> Unit)? = null,
    coverOverlay: @Composable (BoxScope.() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    selectionMode: Boolean = false,
) {
    Row(
        modifier = modifier
            .then(if (isSelected) Modifier.background(KatariTheme.colors.selected) else Modifier)
            .height(LibraryMetrics.listRowHeight)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (coverReplacement != null) {
            Box(
                modifier = coverModifier
                    .fillMaxHeight()
                    .aspectRatio(coverType.ratio),
                content = coverReplacement,
            )
        } else {
            Box(
                modifier = coverModifier
                    .fillMaxHeight()
                    .aspectRatio(coverType.ratio),
            ) {
                coverType(
                    modifier = Modifier
                        .fillMaxHeight()
                        .alpha(coverAlpha),
                    data = coverData,
                    contentScale = coverContentScale,
                    backgroundColor = coverBackgroundColor,
                )
                coverOverlay?.invoke(this)
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .padding(horizontal = KatariTheme.spacing.l)
                .weight(1f),
            role = TextRole.Body,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        BadgeGroup(content = badge)
        coverTrailingAction(
            selectionMode = selectionMode,
            isSelected = isSelected,
            onClickContinueReading = onClickContinueReading,
            continueReadingProgress = continueReadingProgress,
            continueReadingContentDescription = continueReadingContentDescription,
            large = false,
            onCover = false,
        )?.invoke(Modifier.padding(start = KatariTheme.spacing.s))
    }
}
