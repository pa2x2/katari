package eu.kanade.tachiyomi.ui.video.player.subtitle

import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.SubtitlePosition
import tachiyomi.domain.entry.model.SubtitleTextEdge

/**
 * How the viewer wants a title's text subtitles to look. [position] is where they rest, [textSize] a share of the
 * picture's height, the offsets shares of its width and height the subtitles are dragged by from there, and the colours
 * ARGB. A title saves it only once it
 * differs from the default; [normalized] holds every value to what the editor offers.
 */
internal data class VideoSubtitleAppearance(
    val position: SubtitlePosition = SubtitlePosition.BOTTOM_CENTER,
    val offsetX: Float = DEFAULT_SUBTITLE_OFFSET_X,
    val offsetY: Float = DEFAULT_SUBTITLE_OFFSET_Y,
    val textSize: Float = DEFAULT_SUBTITLE_TEXT_SIZE,
    val textColor: Int = DEFAULT_SUBTITLE_TEXT_COLOR,
    val backgroundColor: Int = DEFAULT_SUBTITLE_BACKGROUND_COLOR,
    val backgroundOpacity: Float = DEFAULT_SUBTITLE_BACKGROUND_OPACITY,
    val textEdge: SubtitleTextEdge = SubtitleTextEdge.OUTLINE,
) {
    val isCustomized: Boolean
        get() = normalized() != VideoSubtitleAppearance()

    /** The background colour with its opacity as the alpha. */
    val backgroundArgb: Int
        get() {
            val alpha = (backgroundOpacity.coerceIn(0f, 1f) * 255f).toInt()
            return (alpha shl 24) or (backgroundColor and 0x00FFFFFF)
        }

    fun normalized(): VideoSubtitleAppearance {
        return copy(
            offsetX = offsetX.coerceIn(MIN_SUBTITLE_OFFSET_X, MAX_SUBTITLE_OFFSET_X),
            offsetY = offsetY.coerceIn(MIN_SUBTITLE_OFFSET_Y, MAX_SUBTITLE_OFFSET_Y),
            textSize = textSize.coerceIn(MIN_SUBTITLE_TEXT_SIZE, MAX_SUBTITLE_TEXT_SIZE),
            backgroundOpacity = backgroundOpacity.coerceIn(0f, 1f),
        )
    }
}

internal fun PlaybackPreferences.toSubtitleAppearance(): VideoSubtitleAppearance {
    return VideoSubtitleAppearance(
        position = subtitlePosition ?: SubtitlePosition.BOTTOM_CENTER,
        offsetX = subtitleOffsetX?.toFloat() ?: DEFAULT_SUBTITLE_OFFSET_X,
        offsetY = subtitleOffsetY?.toFloat() ?: DEFAULT_SUBTITLE_OFFSET_Y,
        textSize = subtitleTextSize?.toFloat() ?: DEFAULT_SUBTITLE_TEXT_SIZE,
        textColor = subtitleTextColor ?: DEFAULT_SUBTITLE_TEXT_COLOR,
        backgroundColor = subtitleBackgroundColor ?: DEFAULT_SUBTITLE_BACKGROUND_COLOR,
        backgroundOpacity = subtitleBackgroundOpacity?.toFloat() ?: DEFAULT_SUBTITLE_BACKGROUND_OPACITY,
        textEdge = subtitleTextEdge ?: SubtitleTextEdge.OUTLINE,
    ).normalized()
}

internal fun VideoSubtitleAppearance.toPersistedPosition(): SubtitlePosition? {
    val normalized = normalized()
    return normalized.position.takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedOffsetX(): Double? {
    val normalized = normalized()
    return normalized.offsetX.toDouble().takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedOffsetY(): Double? {
    val normalized = normalized()
    return normalized.offsetY.toDouble().takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedTextSize(): Double? {
    val normalized = normalized()
    return normalized.textSize.toDouble().takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedTextColor(): Int? {
    val normalized = normalized()
    return normalized.textColor.takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedTextEdge(): SubtitleTextEdge? {
    val normalized = normalized()
    return normalized.textEdge.takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedBackgroundColor(): Int? {
    val normalized = normalized()
    return normalized.backgroundColor.takeIf { normalized.isCustomized }
}

internal fun VideoSubtitleAppearance.toPersistedBackgroundOpacity(): Double? {
    val normalized = normalized()
    return normalized.backgroundOpacity.toDouble().takeIf { normalized.isCustomized }
}

internal const val DEFAULT_SUBTITLE_TEXT_SIZE = 0.0533f
internal const val DEFAULT_SUBTITLE_BOTTOM_PADDING_FRACTION = 0.08f
internal const val DEFAULT_SUBTITLE_BACKGROUND_OPACITY = 0.72f
internal const val DEFAULT_SUBTITLE_OFFSET_X = 0f
internal const val DEFAULT_SUBTITLE_OFFSET_Y = 0f
internal const val MIN_SUBTITLE_TEXT_SIZE = 0.03f
internal const val MAX_SUBTITLE_TEXT_SIZE = 0.12f
internal const val MIN_SUBTITLE_OFFSET_X = -0.45f
internal const val MAX_SUBTITLE_OFFSET_X = 0.45f
internal const val MIN_SUBTITLE_OFFSET_Y = -0.78f
internal const val MAX_SUBTITLE_OFFSET_Y = 0.25f
internal const val DEFAULT_SUBTITLE_TEXT_COLOR = 0xFFFFFFFF.toInt()
internal const val DEFAULT_SUBTITLE_BACKGROUND_COLOR = 0xFF000000.toInt()

/** The colours the subtitle editor offers for subtitle text, as ARGB integers. */
internal val SUBTITLE_TEXT_COLOR_PRESETS = listOf(
    DEFAULT_SUBTITLE_TEXT_COLOR,
    0xFFFFFF99.toInt(),
    0xFFFFC857.toInt(),
    0xFF8BE9FD.toInt(),
    0xFFFF79C6.toInt(),
)

/** The colours the subtitle editor offers for the box behind subtitle text, as ARGB integers. */
internal val SUBTITLE_BACKGROUND_COLOR_PRESETS = listOf(
    DEFAULT_SUBTITLE_BACKGROUND_COLOR,
    0xFF1F2937.toInt(),
    0xFF374151.toInt(),
    0xFF7F1D1D.toInt(),
    0xFF0F766E.toInt(),
)
