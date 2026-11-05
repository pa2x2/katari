package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import mihon.presentation.kit.content.SubtitleEdge
import mihon.presentation.kit.content.SubtitleLook
import mihon.presentation.kit.content.SubtitleTextLayout
import mihon.presentation.kit.content.drawSubtitleText
import mihon.presentation.kit.content.measureSubtitleText
import mihon.video.engine.spi.player.SubtitleCue
import mihon.video.engine.spi.player.SubtitleImage
import mihon.video.engine.spi.player.SubtitleTextAlignment
import tachiyomi.domain.entry.model.SubtitleTextEdge
import kotlin.math.roundToInt

/**
 * Draws text and picture subtitle [cues] in the viewer's [appearance], where [layout] puts them for a drawing of the
 * given width and height in pixels. The player's own and the subtitle editor's subtitles both come from here, on every
 * platform; subtitles an engine draws itself never reach it.
 *
 * [layout] is read while drawing, so what it reads, such as a framing a pinch is changing, redraws the subtitles
 * without composing them again.
 */
@Composable
internal fun VideoSubtitles(
    cues: List<SubtitleCue>,
    appearance: VideoSubtitleAppearance,
    layout: (width: Float, height: Float) -> VideoSubtitleLayout,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer(cacheSize = TEXT_LAYOUT_CACHE_SIZE)
    val images = remember(cues) {
        cues.mapNotNull(SubtitleCue::image).associateWith(SubtitleImage::toImageBitmap)
    }
    Spacer(
        modifier = modifier
            .clipToBounds()
            .drawBehind {
                if (cues.isEmpty()) return@drawBehind
                val current = layout(size.width, size.height)
                val look = SubtitleLook(
                    textSizePx = current.textSizePx,
                    textArgb = appearance.textColor,
                    backgroundArgb = appearance.backgroundArgb,
                    edge = appearance.textEdge.toSubtitleEdge(),
                )
                if (look.textSizePx <= 0f) return@drawBehind
                val placed = placeSubtitles(cues, current, boundsWidth = size.width) { cue, width, fixedWidth ->
                    MeasuredText(
                        measurer.measureSubtitleText(
                            text = cue.text,
                            look = look,
                            width = width,
                            alignment = cue.textAlignment.toTextAlign(),
                            fixedWidth = fixedWidth,
                            density = this,
                        ),
                    )
                }
                placed.forEach { subtitle ->
                    when (subtitle) {
                        is PlacedSubtitle.Text -> drawSubtitleText(
                            subtitle.text.layout,
                            Offset(subtitle.left, subtitle.top),
                        )
                        is PlacedSubtitle.Image -> images[subtitle.image]?.let { bitmap ->
                            drawImage(
                                image = bitmap,
                                dstOffset = IntOffset(subtitle.left.roundToInt(), subtitle.top.roundToInt()),
                                dstSize = IntSize(subtitle.width.roundToInt(), subtitle.height.roundToInt()),
                            )
                        }
                    }
                }
            },
    )
}

private class MeasuredText(val layout: SubtitleTextLayout) : MeasuredSubtitleText {
    override val width: Int get() = layout.width
    override val height: Int get() = layout.height
    override val linesWidth: Int get() = layout.linesWidth
    override val firstLineHeight: Float get() = layout.firstLineHeight
}

private fun SubtitleTextEdge.toSubtitleEdge(): SubtitleEdge = when (this) {
    SubtitleTextEdge.NONE -> SubtitleEdge.None
    SubtitleTextEdge.OUTLINE -> SubtitleEdge.Outline
    SubtitleTextEdge.SHADOW -> SubtitleEdge.Shadow
}

private fun SubtitleTextAlignment.toTextAlign(): TextAlign = when (this) {
    SubtitleTextAlignment.Start -> TextAlign.Start
    SubtitleTextAlignment.Centre -> TextAlign.Center
    SubtitleTextAlignment.End -> TextAlign.End
}

// Each cue is measured twice, at the area's width and at its own, and a few cues show at once
private const val TEXT_LAYOUT_CACHE_SIZE = 16
