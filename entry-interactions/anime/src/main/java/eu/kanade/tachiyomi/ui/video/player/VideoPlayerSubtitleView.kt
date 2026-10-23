package eu.kanade.tachiyomi.ui.video.player

import android.graphics.Color
import androidx.media3.common.Player
import androidx.media3.common.text.Cue
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView

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
    setBottomPaddingFraction(DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION)
    setStyle(
        CaptionStyleCompat(
            normalizedAppearance.textColor,
            withAlpha(normalizedAppearance.backgroundColor, normalizedAppearance.backgroundOpacity),
            Color.TRANSPARENT,
            CaptionStyleCompat.EDGE_TYPE_OUTLINE,
            Color.BLACK,
            null,
        ),
    )
    setFractionalTextSize(normalizedAppearance.textSize)
    post {
        translationX = width * normalizedAppearance.offsetX
        translationY = height * normalizedAppearance.offsetY
    }
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
