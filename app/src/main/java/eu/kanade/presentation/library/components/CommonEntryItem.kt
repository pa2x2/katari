package eu.kanade.presentation.library.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.entry.components.EntryCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.BadgeGroup
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.domain.entry.model.EntryCover as EntryCoverModel

object CommonEntryItemDefaults {
    val GridHorizontalSpacer = 4.dp
    val GridVerticalSpacer = 4.dp

    @Suppress("ConstPropertyName")
    const val BrowseFavoriteCoverAlpha = 0.34f
}

private val ContinueReadingButtonSizeSmall = 28.dp
private val ContinueReadingButtonSizeLarge = 32.dp

private val ContinueReadingButtonIconSizeSmall = 16.dp
private val ContinueReadingButtonIconSizeLarge = 20.dp

private val ContinueReadingButtonGridPadding = 6.dp
private val ContinueReadingButtonListSpacing = 8.dp
private val ContinueReadingProgressStrokeWidth = 2.dp

private const val GRID_SELECTED_COVER_ALPHA = 0.76f

/**
 * Layout of grid list item with title overlaying the cover.
 * Accepts null [title] for a cover-only view.
 */
@Composable
fun EntryCompactGridItem(
    coverData: EntryCoverModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean = false,
    title: String? = null,
    coverType: EntryCover = EntryCover.Book,
    coverContentScale: ContentScale = ContentScale.Crop,
    coverBackgroundColor: Color = Color.Transparent,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    coverAlpha: Float = 1f,
    coverReplacement: @Composable (BoxScope.() -> Unit)? = null,
    coverOverlay: @Composable (BoxScope.() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
    coverBadgeStart: @Composable (RowScope.() -> Unit)? = null,
    coverBadgeEnd: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
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
                            .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                        data = coverData,
                        contentScale = coverContentScale,
                        backgroundColor = coverBackgroundColor,
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
                        backgroundColor = coverBackgroundColor,
                        onClickContinueReading = onClickContinueReading,
                        continueReadingProgress = continueReadingProgress,
                        continueReadingContentDescription = continueReadingContentDescription,
                    )
                } else if (onClickContinueReading != null) {
                    ContinueReadingButton(
                        size = ContinueReadingButtonSizeLarge,
                        iconSize = ContinueReadingButtonIconSizeLarge,
                        onClick = onClickContinueReading,
                        progress = continueReadingProgress,
                        contentDescription = continueReadingContentDescription,
                        modifier = Modifier
                            .padding(ContinueReadingButtonGridPadding)
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
    backgroundColor: Color = Color.Transparent,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
            .background(
                Brush.verticalGradient(
                    0f to backgroundColor,
                    1f to Color(0xAA000000).compositeOver(backgroundColor),
                ),
            )
            .fillMaxHeight(0.33f)
            .fillMaxWidth()
            .align(Alignment.BottomCenter),
    )
    Row(
        modifier = Modifier.align(Alignment.BottomStart),
        verticalAlignment = Alignment.Bottom,
    ) {
        GridItemTitle(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            title = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 4f,
                ),
            ),
            minLines = 1,
        )
        if (onClickContinueReading != null) {
            ContinueReadingButton(
                size = ContinueReadingButtonSizeSmall,
                iconSize = ContinueReadingButtonIconSizeSmall,
                onClick = onClickContinueReading,
                progress = continueReadingProgress,
                contentDescription = continueReadingContentDescription,
                modifier = Modifier.padding(
                    end = ContinueReadingButtonGridPadding,
                    bottom = ContinueReadingButtonGridPadding,
                ),
            )
        }
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
    isSelected: Boolean = false,
    titleMaxLines: Int = 2,
    coverType: EntryCover = EntryCover.Book,
    coverContentScale: ContentScale = ContentScale.Crop,
    coverBackgroundColor: Color = Color.Transparent,
    coverAlpha: Float = 1f,
    coverReplacement: @Composable (BoxScope.() -> Unit)? = null,
    coverOverlay: @Composable (BoxScope.() -> Unit)? = null,
    coverModifier: Modifier = Modifier,
    coverBadgeStart: (@Composable RowScope.() -> Unit)? = null,
    coverBadgeEnd: (@Composable RowScope.() -> Unit)? = null,
    onClickContinueReading: (() -> Unit)? = null,
    continueReadingProgress: Float? = null,
    continueReadingContentDescription: StringResource = MR.strings.action_resume,
    modifier: Modifier = Modifier,
) {
    GridItemSelectable(
        isSelected = isSelected,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
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
                                .alpha(if (isSelected) GRID_SELECTED_COVER_ALPHA else coverAlpha),
                            data = coverData,
                            contentScale = coverContentScale,
                            backgroundColor = coverBackgroundColor,
                        )
                    }
                },
                badgesStart = coverBadgeStart,
                badgesEnd = coverBadgeEnd,
                content = {
                    coverOverlay?.invoke(this)
                    if (onClickContinueReading != null) {
                        ContinueReadingButton(
                            size = ContinueReadingButtonSizeLarge,
                            iconSize = ContinueReadingButtonIconSizeLarge,
                            onClick = onClickContinueReading,
                            progress = continueReadingProgress,
                            contentDescription = continueReadingContentDescription,
                            modifier = Modifier
                                .padding(ContinueReadingButtonGridPadding)
                                .align(Alignment.BottomEnd),
                        )
                    }
                },
            )
            GridItemTitle(
                modifier = Modifier.padding(4.dp),
                title = title,
                style = MaterialTheme.typography.titleSmall,
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
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                content = badgesStart,
            )
        }

        if (badgesEnd != null) {
            BadgeGroup(
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopEnd),
                content = badgesEnd,
            )
        }
    }
}

@Composable
private fun GridItemTitle(
    title: String,
    style: TextStyle,
    minLines: Int,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
) {
    Text(
        modifier = modifier,
        text = title,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        minLines = minLines,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = style,
    )
}

/**
 * Wrapper for grid items to handle selection state, click and long click.
 */
@Composable
private fun GridItemSelectable(
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .selectedOutline(isSelected = isSelected, color = MaterialTheme.colorScheme.secondary)
            .padding(4.dp),
    ) {
        val contentColor = if (isSelected) {
            MaterialTheme.colorScheme.onSecondary
        } else {
            LocalContentColor.current
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

/**
 * @see GridItemSelectable
 */
private fun Modifier.selectedOutline(
    isSelected: Boolean,
    color: Color,
) = drawBehind { if (isSelected) drawRect(color = color) }

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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .selectedBackground(isSelected)
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                .padding(horizontal = 16.dp)
                .weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
        BadgeGroup(content = badge)
        if (onClickContinueReading != null) {
            ContinueReadingButton(
                size = ContinueReadingButtonSizeSmall,
                iconSize = ContinueReadingButtonIconSizeSmall,
                onClick = onClickContinueReading,
                progress = continueReadingProgress,
                contentDescription = continueReadingContentDescription,
                modifier = Modifier.padding(start = ContinueReadingButtonListSpacing),
            )
        }
    }
}

@Composable
private fun ContinueReadingButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    progress: Float? = null,
    contentDescription: StringResource = MR.strings.action_resume,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        FilledIconButton(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                contentColor = contentColorFor(MaterialTheme.colorScheme.primaryContainer),
            ),
            modifier = Modifier.size(size),
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(contentDescription),
                modifier = Modifier.size(iconSize),
            )
        }
        if (progress != null) {
            ButtonProgressIndicator(
                progress = progress,
                size = size,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

@Composable
private fun ButtonProgressIndicator(
    progress: Float,
    size: Dp,
    color: Color,
    trackColor: Color,
    shape: Shape,
) {
    val coercedProgress = progress.takeUnless { it.isNaN() }?.coerceIn(0f, 1f) ?: 0f
    Canvas(
        modifier = Modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = coercedProgress,
                    range = 0f..1f,
                )
            },
    ) {
        val outlinePath = Path().apply {
            when (val outline = shape.createOutline(this@Canvas.size, layoutDirection, this@Canvas)) {
                is Outline.Rectangle -> addRect(outline.rect, Path.Direction.Clockwise)
                is Outline.Rounded -> addRoundRect(outline.roundRect, Path.Direction.Clockwise)
                is Outline.Generic -> addPath(outline.path)
            }
        }
        val pathMeasure = PathMeasure().apply {
            setPath(outlinePath, forceClosed = true)
        }
        val pathLength = pathMeasure.length
        val startDistance = (0..100)
            .minBy { sample ->
                val position = pathMeasure.getPosition(pathLength * sample / 100f)
                val delta = position - Offset(this.size.width / 2f, 0f)
                delta.getDistanceSquared()
            }
            .let { pathLength * it / 100f }
        val progressPath = Path()
        val progressLength = pathLength * coercedProgress
        val firstSegmentLength = minOf(progressLength, pathLength - startDistance)

        if (coercedProgress == 1f) {
            progressPath.addPath(outlinePath)
        } else {
            pathMeasure.getSegment(
                startDistance = startDistance,
                stopDistance = startDistance + firstSegmentLength,
                destination = progressPath,
            )
            if (progressLength > firstSegmentLength) {
                pathMeasure.getSegment(
                    startDistance = 0f,
                    stopDistance = progressLength - firstSegmentLength,
                    destination = progressPath,
                )
            }
        }

        val stroke = Stroke(
            // The path lies on the button boundary, so draw double width and clip the outer half.
            width = 2 * ContinueReadingProgressStrokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        clipPath(outlinePath) {
            drawPath(path = outlinePath, color = trackColor, style = stroke)
            drawPath(path = progressPath, color = color, style = stroke)
        }
    }
}
