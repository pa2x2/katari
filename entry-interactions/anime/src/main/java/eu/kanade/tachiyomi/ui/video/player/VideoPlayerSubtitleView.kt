package eu.kanade.tachiyomi.ui.video.player

import android.graphics.Color
import android.util.TypedValue
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleLayout
import eu.kanade.tachiyomi.ui.video.player.subtitle.viewHeight
import tachiyomi.domain.entry.model.SubtitleTextEdge

internal fun PlayerView.applySubtitleAppearance(
    appearance: VideoSubtitleAppearance,
    editorVisible: Boolean = false,
) {
    val subtitleView = getSubtitleView() ?: return
    subtitleView.applyAppearance(appearance)
    subtitleView.visibility = if (editorVisible) android.view.View.INVISIBLE else android.view.View.VISIBLE
}

internal fun SubtitleView.applyAppearance(appearance: VideoSubtitleAppearance) {
    val normalizedAppearance = appearance.normalized()
    setApplyEmbeddedStyles(false)
    setApplyEmbeddedFontSizes(false)
    setViewType(SubtitleView.VIEW_TYPE_CANVAS)
    setStyle(
        CaptionStyleCompat(
            normalizedAppearance.textColor,
            withAlpha(normalizedAppearance.backgroundColor, normalizedAppearance.backgroundOpacity),
            Color.TRANSPARENT,
            when (normalizedAppearance.textEdge) {
                SubtitleTextEdge.NONE -> CaptionStyleCompat.EDGE_TYPE_NONE
                SubtitleTextEdge.OUTLINE -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
                SubtitleTextEdge.SHADOW -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
            },
            Color.BLACK,
            null,
        ),
    )
}

/**
 * Sizes and places the subtitles as [layout] says, moved by the [appearance]'s offsets, in a view that covers the
 * player or, unless [viewCoversPlayer], the fitted picture centred in it.
 */
internal fun SubtitleView.applyLayout(
    layout: VideoSubtitleLayout,
    appearance: VideoSubtitleAppearance,
    viewCoversPlayer: Boolean,
) {
    val normalizedAppearance = appearance.normalized()
    setFixedTextSize(TypedValue.COMPLEX_UNIT_PX, layout.textSizePx)
    val viewHeight = layout.viewHeight(viewCoversPlayer)
    if (viewHeight > 0f) {
        val viewBottomPx = (layout.playerHeight - viewHeight) / 2f
        setBottomPaddingFraction(((layout.bottomPx - viewBottomPx) / viewHeight).coerceIn(0f, 1f))
    }
    translationX = layout.offsetBaseWidth * normalizedAppearance.offsetX
    translationY = layout.offsetBaseHeight * normalizedAppearance.offsetY
}

internal fun Player.subtitlePreviewText(): String? {
    val cues = currentCues.cues
    return cues.firstNotNullOfOrNull { cue -> cue.text?.toString()?.trim()?.takeIf(String::isNotBlank) }
}

internal fun Player.subtitlePreviewCues(): List<Cue> {
    return currentCues.cues.filter { cue ->
        cue.text?.toString()?.trim()?.isNotBlank() == true
    }
}

internal fun withAlpha(color: Int, opacity: Float): Int {
    val alpha = (opacity.coerceIn(0f, 1f) * 255f).toInt()
    return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
