package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.icerock.moko.resources.StringResource
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.LibraryMetrics
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Resumes an entry from its cover or row; with [progress] (0 to 1) the button's outline fills with how far it got. */
@Composable
internal fun ContinueReadingButton(
    size: Dp,
    iconSize: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    contentDescription: StringResource = MR.strings.action_resume,
) {
    val shape = KatariTheme.shapes.small
    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(size),
            container = SurfaceContainer.PrimaryContainer,
            shape = shape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.PlayArrow,
                    contentDescription = stringResource(contentDescription),
                    modifier = Modifier.size(iconSize),
                )
            }
        }
        if (progress != null) {
            ProgressOutline(
                progress = progress,
                size = size,
                color = KatariTheme.colors.primary,
                trackColor = KatariTheme.colors.surfaceContainerHighest,
                shape = shape,
            )
        }
    }
}

@Composable
private fun ProgressOutline(
    progress: Float,
    size: Dp,
    color: Color,
    trackColor: Color,
    shape: Shape,
) {
    val coercedProgress = progress.takeUnless { it.isNaN() }?.coerceIn(0f, 1f) ?: 0f
    Box(
        modifier = Modifier
            .size(size)
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = coercedProgress,
                    range = 0f..1f,
                )
            }
            .drawWithCache {
                val outlinePath = Path().apply {
                    when (
                        val outline = shape.createOutline(
                            this@drawWithCache.size,
                            layoutDirection,
                            this@drawWithCache,
                        )
                    ) {
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
                        val delta = position - Offset(this@drawWithCache.size.width / 2f, 0f)
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
                    width = 2 * LibraryMetrics.resumeProgressStroke.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
                onDrawBehind {
                    clipPath(outlinePath) {
                        drawPath(path = outlinePath, color = trackColor, style = stroke)
                        drawPath(path = progressPath, color = color, style = stroke)
                    }
                }
            },
    )
}
