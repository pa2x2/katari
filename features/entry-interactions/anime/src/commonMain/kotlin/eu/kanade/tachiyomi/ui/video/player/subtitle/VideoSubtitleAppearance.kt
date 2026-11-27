package eu.kanade.tachiyomi.ui.video.player.subtitle

import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.BUILT_IN_SUBTITLE_BACKGROUND_COLOR
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.BUILT_IN_SUBTITLE_BACKGROUND_OPACITY
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.BUILT_IN_SUBTITLE_TEXT_COLOR
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.BUILT_IN_SUBTITLE_TEXT_SIZE
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.MAX_SUBTITLE_TEXT_SIZE
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.MIN_SUBTITLE_TEXT_SIZE
import tachiyomi.domain.entry.model.PlaybackPreferences
import tachiyomi.domain.entry.model.SubtitlePosition
import tachiyomi.domain.entry.model.SubtitleTextEdge

/**
 * How the viewer wants text subtitles to look. [position] is where they rest, [textSize] a share of the picture's
 * height, the offsets shares of its width and height the subtitles are dragged by from there, and the colours ARGB.
 * The constructor's values are the built-in look; [normalized] holds every value to what the editor offers.
 *
 * A series either keeps a look of its own ([ownSubtitleLook]) or follows the default look ([subtitleDefault]), which
 * every series without one shares. The default has no offsets: where subtitles are dragged suits one series' pictures,
 * not every series'.
 */
internal data class VideoSubtitleAppearance(
    val position: SubtitlePosition = SubtitlePosition.BOTTOM_CENTER,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val textSize: Float = BUILT_IN_SUBTITLE_TEXT_SIZE,
    val textColor: Int = BUILT_IN_SUBTITLE_TEXT_COLOR,
    val backgroundColor: Int = BUILT_IN_SUBTITLE_BACKGROUND_COLOR,
    val backgroundOpacity: Float = BUILT_IN_SUBTITLE_BACKGROUND_OPACITY,
    val textEdge: SubtitleTextEdge = SubtitleTextEdge.OUTLINE,
) {
    /** The subtitles sit where [position] rests them, not dragged off it. */
    val restsInPlace: Boolean
        get() = offsetX == 0f && offsetY == 0f

    fun restingIn(position: SubtitlePosition): VideoSubtitleAppearance =
        copy(position = position, offsetX = 0f, offsetY = 0f)

    /** The background colour with its opacity as the alpha. */
    val backgroundArgb: Int
        get() {
            val alpha = (backgroundOpacity.coerceIn(0f, 1f) * 255f).toInt()
            return (alpha shl 24) or (backgroundColor and 0x00FFFFFF)
        }

    /** The part of the look a default keeps: everything but where the subtitles are dragged off their grid cell. */
    fun asDefault(): VideoSubtitleAppearance = restingIn(position).normalized()

    fun normalized(): VideoSubtitleAppearance {
        return copy(
            offsetX = offsetX.coerceIn(-SUBTITLE_OFFSET_LIMIT, SUBTITLE_OFFSET_LIMIT),
            offsetY = offsetY.coerceIn(-SUBTITLE_OFFSET_LIMIT, SUBTITLE_OFFSET_LIMIT),
            textSize = textSize.coerceIn(MIN_SUBTITLE_TEXT_SIZE, MAX_SUBTITLE_TEXT_SIZE),
            backgroundOpacity = backgroundOpacity.coerceIn(0f, 1f),
        )
    }
}

internal fun AnimePlayerPreferences.subtitleDefault(): VideoSubtitleAppearance {
    return VideoSubtitleAppearance(
        position = defaultSubtitlePosition.get(),
        textSize = defaultSubtitleTextSize.get(),
        textColor = defaultSubtitleTextColor.get(),
        backgroundColor = defaultSubtitleBackgroundColor.get(),
        backgroundOpacity = defaultSubtitleBackgroundOpacity.get(),
        textEdge = defaultSubtitleTextEdge.get(),
    ).normalized()
}

/** Makes [appearance] the default look; the built-in look clears it. */
internal fun AnimePlayerPreferences.setSubtitleDefault(appearance: VideoSubtitleAppearance) {
    val look = appearance.asDefault()
    if (look == VideoSubtitleAppearance()) {
        resetDefaultSubtitleLook()
        return
    }
    defaultSubtitlePosition.set(look.position)
    defaultSubtitleTextSize.set(look.textSize)
    defaultSubtitleTextColor.set(look.textColor)
    defaultSubtitleBackgroundColor.set(look.backgroundColor)
    defaultSubtitleBackgroundOpacity.set(look.backgroundOpacity)
    defaultSubtitleTextEdge.set(look.textEdge)
}

/** The series' own look, or null while it follows the default. */
internal fun PlaybackPreferences.ownSubtitleLook(): VideoSubtitleAppearance? {
    val columns = listOf(
        subtitlePosition,
        subtitleOffsetX,
        subtitleOffsetY,
        subtitleTextSize,
        subtitleTextColor,
        subtitleBackgroundColor,
        subtitleBackgroundOpacity,
        subtitleTextEdge,
    )
    if (columns.all { it == null }) return null
    // A look saved before positions existed has none, and rested at the bottom centre
    val builtIn = VideoSubtitleAppearance()
    return VideoSubtitleAppearance(
        position = subtitlePosition ?: builtIn.position,
        offsetX = subtitleOffsetX?.toFloat() ?: builtIn.offsetX,
        offsetY = subtitleOffsetY?.toFloat() ?: builtIn.offsetY,
        textSize = subtitleTextSize?.toFloat() ?: builtIn.textSize,
        textColor = subtitleTextColor ?: builtIn.textColor,
        backgroundColor = subtitleBackgroundColor ?: builtIn.backgroundColor,
        backgroundOpacity = subtitleBackgroundOpacity?.toFloat() ?: builtIn.backgroundOpacity,
        textEdge = subtitleTextEdge ?: builtIn.textEdge,
    ).normalized()
}

/** The series keeps [own] as its look, or follows the default while it is null. */
internal fun PlaybackPreferences.withOwnSubtitleLook(own: VideoSubtitleAppearance?): PlaybackPreferences = copy(
    subtitlePosition = own?.position,
    subtitleOffsetX = own?.offsetX?.toDouble(),
    subtitleOffsetY = own?.offsetY?.toDouble(),
    subtitleTextSize = own?.textSize?.toDouble(),
    subtitleTextColor = own?.textColor,
    subtitleBackgroundColor = own?.backgroundColor,
    subtitleBackgroundOpacity = own?.backgroundOpacity?.toDouble(),
    subtitleTextEdge = own?.textEdge,
)

// How far subtitles rest from the picture's edges, a share of its height
internal const val SUBTITLE_EDGE_FRACTION = 0.08f

// Far enough to cross the picture from any cell; the player holds the lines inside it whatever the offset
internal const val SUBTITLE_OFFSET_LIMIT = 1f

/** The colours the subtitle editor offers for subtitle text, as ARGB integers. */
internal val SUBTITLE_TEXT_COLOR_PRESETS = listOf(
    BUILT_IN_SUBTITLE_TEXT_COLOR,
    0xFFFFFF99.toInt(),
    0xFFFFC857.toInt(),
    0xFF8BE9FD.toInt(),
    0xFFFF79C6.toInt(),
)

/** The colours the subtitle editor offers for the box behind subtitle text, as ARGB integers. */
internal val SUBTITLE_BACKGROUND_COLOR_PRESETS = listOf(
    BUILT_IN_SUBTITLE_BACKGROUND_COLOR,
    0xFF1F2937.toInt(),
    0xFF374151.toInt(),
    0xFF7F1D1D.toInt(),
    0xFF0F766E.toInt(),
)
