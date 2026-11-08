package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.text.Layout
import androidx.media3.common.text.Cue
import app.katari.video.engine.spi.player.SubtitleAnchored
import app.katari.video.engine.spi.player.SubtitleCue
import app.katari.video.engine.spi.player.SubtitleImage
import app.katari.video.engine.spi.player.SubtitleLine
import app.katari.video.engine.spi.player.SubtitleTextAlignment

/**
 * Media3's cues as the app draws them, placed as Media3's own subtitle view placed them, with its embedded styles and
 * font sizes left out, which the viewer's appearance replaces.
 *
 * Media3 gives a cue its file left unplaced a row counted up from the bottom (-1, -2, …), so cues on screen together
 * don't cover each other. Those rows become cues without a line, in the same order from the bottom, so they rest where
 * the app puts subtitles: above the controls, or in the band under the picture. Vertical cues are drawn across, as
 * Media3's canvas drawing did.
 */
internal fun List<Cue>.toSubtitleCues(): List<SubtitleCue> =
    asSequence()
        .map { if (it.verticalType != Cue.TYPE_UNSET) it.drawnAcross() else it }
        .withIndex()
        // Stable, so unplaced cues without a row keep their order
        .sortedBy { (index, cue) -> cue.bottomRow() ?: index }
        .mapNotNull { (_, cue) -> cue.toSubtitleCue() }
        .toList()

private fun Cue.toSubtitleCue(): SubtitleCue? {
    val bitmap = bitmap
    if (bitmap != null) {
        if (line == Cue.DIMEN_UNSET || position == Cue.DIMEN_UNSET || size == Cue.DIMEN_UNSET) return null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return SubtitleCue(
            text = "",
            line = SubtitleLine.Share(SubtitleAnchored(line, lineAnchor.toAnchor())),
            position = SubtitleAnchored(position, positionAnchor.toAnchor()),
            width = size,
            image = SubtitleImage(
                pixels = pixels,
                width = bitmap.width,
                height = bitmap.height,
                heightShare = bitmapHeight.takeUnless { it == Cue.DIMEN_UNSET },
            ),
        )
    }
    val text = text?.toString()?.takeIf(String::isNotEmpty) ?: return null
    return SubtitleCue(
        text = text,
        textAlignment = when (textAlignment) {
            Layout.Alignment.ALIGN_NORMAL -> SubtitleTextAlignment.Start
            Layout.Alignment.ALIGN_OPPOSITE -> SubtitleTextAlignment.End
            else -> SubtitleTextAlignment.Centre
        },
        line = when {
            line == Cue.DIMEN_UNSET || bottomRow() != null -> null
            lineType == Cue.LINE_TYPE_NUMBER -> SubtitleLine.Row(line)
            else -> SubtitleLine.Share(SubtitleAnchored(line, lineAnchor.toAnchor()))
        },
        position = position.takeUnless { it == Cue.DIMEN_UNSET }
            ?.let { SubtitleAnchored(it, positionAnchor.toAnchor()) },
        width = size.takeUnless { it == Cue.DIMEN_UNSET },
    )
}

/** For a text cue on a row counted from the bottom, which row: 0 for the lowest. */
private fun Cue.bottomRow(): Int? =
    if (bitmap == null && lineType == Cue.LINE_TYPE_NUMBER && line != Cue.DIMEN_UNSET && line < 0f) {
        (-line - 1f).toInt()
    } else {
        null
    }

private fun Int.toAnchor(): SubtitleAnchored.Anchor = when (this) {
    Cue.ANCHOR_TYPE_MIDDLE -> SubtitleAnchored.Anchor.Middle
    Cue.ANCHOR_TYPE_END -> SubtitleAnchored.Anchor.End
    else -> SubtitleAnchored.Anchor.Start
}

/** A vertical cue turned across, its line counted from the other side, as Media3's canvas drawing turned it. */
private fun Cue.drawnAcross(): Cue {
    val builder = buildUpon()
        .setPosition(Cue.DIMEN_UNSET)
        .setPositionAnchor(Cue.TYPE_UNSET)
        .setTextAlignment(null)
    when {
        line == Cue.DIMEN_UNSET -> Unit
        lineType == Cue.LINE_TYPE_FRACTION -> builder.setLine(1f - line, Cue.LINE_TYPE_FRACTION)
        else -> builder.setLine(-line - 1f, Cue.LINE_TYPE_NUMBER)
    }
    when (lineAnchor) {
        Cue.ANCHOR_TYPE_END -> builder.setLineAnchor(Cue.ANCHOR_TYPE_START)
        Cue.ANCHOR_TYPE_START -> builder.setLineAnchor(Cue.ANCHOR_TYPE_END)
    }
    return builder.build()
}
