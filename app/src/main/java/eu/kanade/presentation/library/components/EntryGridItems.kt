package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryCover
import mihon.presentation.kit.content.BadgeGroup
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.coverFrame
import mihon.presentation.kit.surface.CoverCell
import mihon.presentation.kit.surface.MediaScrim
import mihon.presentation.kit.surface.MediaScrimEdge
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.domain.entry.model.EntryCover as EntryCoverModel

/** Share of a compact cell's cover that the shade behind its title covers, from the bottom edge. */
private const val COVER_TITLE_SCRIM_FRACTION = 0.33f

/**
 * Layout of grid list item with title overlaying the cover.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun EntryCompactGridItem(
    coverData: EntryCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    title: String? = null,
    coverType: EntryCover = EntryCover.Book,
    coverContentScale: ContentScale = ContentScale.Crop,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    coverAlpha: Float = 1f,
    coverReplacement: @Composable (BoxScope.() -> Unit)? = null,
    coverOverlay: @Composable (BoxScope.() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
    selectionMode: Boolean = false,
) {
    CoverCell(
        onClick = onClick,
        modifier = modifier,
        selected = isSelected,
        onLongClick = onLongClick,
    ) {
        EntryGridCover(
            modifier = coverModifier,
            coverType = coverType,
            cover = {
                if (coverReplacement != null) {
                    coverReplacement()
                } else {
                    coverType(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(coverAlpha),
                        data = coverData,
                        contentScale = coverContentScale,
                    )
                }
            },
            badgesStart = coverBadgeStart,
            badgesEnd = coverBadgeEnd,
            content = {
                coverOverlay?.invoke(this)
                if (title != null) {
                    CoverTextOverlay(
                        title = title,
                        trailingAction = coverTrailingAction(
                            selectionMode = selectionMode,
                            isSelected = isSelected,
                            onClickContinueReading = onClickContinueReading,
                            continueReadingProgress = continueReadingProgress,
                            continueReadingContentDescription = continueReadingContentDescription,
                            large = false,
                        ),
                    )
                } else {
                    coverTrailingAction(
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onClickContinueReading = onClickContinueReading,
                        continueReadingProgress = continueReadingProgress,
                        continueReadingContentDescription = continueReadingContentDescription,
                        large = true,
                    )?.invoke(
                        Modifier
                            .padding(KatariTheme.spacing.s)
                            .align(Alignment.BottomEnd),
                    )
                }
            },
        )
    }
}

/**
 * Title overlay for [EntryCompactGridItem]
 */
@Composable
private fun BoxScope.CoverTextOverlay(
    title: String,
    trailingAction: (@Composable (Modifier) -> Unit)? = null,
) {
    MediaScrim(
        edge = MediaScrimEdge.Bottom,
        modifier = Modifier
            .coverFrame()
            .fillMaxHeight(COVER_TITLE_SCRIM_FRACTION)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    ) {}
    Row(
        modifier = Modifier.align(Alignment.BottomStart),
        verticalAlignment = Alignment.Bottom,
    ) {
        GridItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(KatariTheme.spacing.s),
            title = title,
            tone = ContentTone.OnMedia,
            minLines = 1,
        )
        trailingAction?.invoke(
            Modifier.padding(
                end = KatariTheme.spacing.s,
                bottom = KatariTheme.spacing.s,
            ),
        )
    }
}

/**
 * Layout of grid list item with title below the cover.
 */
@Composable
fun EntryComfortableGridItem(
    coverData: EntryCoverModel,
    title: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    titleMaxLines: Int = 2,
    coverType: EntryCover = EntryCover.Book,
    coverContentScale: ContentScale = ContentScale.Crop,
    coverAlpha: Float = 1f,
    coverReplacement: @Composable (BoxScope.() -> Unit)? = null,
    coverOverlay: @Composable (BoxScope.() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    selectionMode: Boolean = false,
) {
    CoverCell(
        onClick = onClick,
        modifier = modifier,
        selected = isSelected,
        onLongClick = onLongClick,
    ) {
        Column {
            EntryGridCover(
                modifier = coverModifier,
                coverType = coverType,
                cover = {
                    if (coverReplacement != null) {
                        coverReplacement()
                    } else {
                        coverType(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(coverAlpha),
                            data = coverData,
                            contentScale = coverContentScale,
                        )
                    }
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
                content = {
                    coverOverlay?.invoke(this)
                    coverTrailingAction(
                        selectionMode = selectionMode,
                        isSelected = isSelected,
                        onClickContinueReading = onClickContinueReading,
                        continueReadingProgress = continueReadingProgress,
                        continueReadingContentDescription = continueReadingContentDescription,
                        large = true,
                    )?.invoke(
                        Modifier
                            .padding(KatariTheme.spacing.s)
                            .align(Alignment.BottomEnd),
                    )
                },
            )
            GridItemTitle(
                modifier = Modifier.padding(horizontal = KatariTheme.spacing.xxs).padding(top = KatariTheme.spacing.s),
                title = title,
                minLines = 2,
                maxLines = titleMaxLines,
            )
        }
    }
}

/**
 * Common cover layout to add contents to be drawn on top of the cover.
 */
@Composable
private fun EntryGridCover(
    modifier: Modifier = Modifier,
    coverType: EntryCover = EntryCover.Book,
    cover: @Composable BoxScope.() -> Unit = {},
    badgesStart: (@Composable RowScope.() -> Unit)? = null,
    badgesEnd: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(coverType.ratio),
    ) {
        cover()
        content?.invoke(this)
        if (badgesStart != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(KatariTheme.spacing.s)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(KatariTheme.spacing.s)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

@Composable
private fun GridItemTitle(
    title: String,
    minLines: Int,
    modifier: Modifier = Modifier,
    tone: ContentTone = ContentTone.Inherit,
    maxLines: Int = 2,
) {
    Text(
        modifier = modifier,
        text = title,
        role = TextRole.CoverTitle,
        tone = tone,
        minLines = minLines,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
