package eu.kanade.tachiyomi.data.backup.conversion.aniyomi.settings

import app.katari.entry.interactions.settings.AnimePlayerPreferences
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport.Item
import eu.kanade.tachiyomi.data.backup.conversion.OtherAppSettingLabel
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import tachiyomi.domain.entry.model.SubtitlePosition
import tachiyomi.domain.entry.model.SubtitleTextEdge
import tachiyomi.i18n.*

/**
 * Aniyomi's subtitle look, set in the video player, as Katari's default subtitle look: size, text colour, background,
 * edge and height. Its font, border and alignment stay listed.
 */
internal fun AniyomiSettings.convertSubtitleSettings() {
    convertTextSize()
    take("pref_text_color_subtitles")?.let { write(AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_COLOR_KEY, it) }
    (take("pref_background_color_subtitles") as? IntPreferenceValue)?.let { background ->
        // Katari keeps the background's opacity apart from its colour
        val alpha = background.value ushr 24
        write(
            AnimePlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_COLOR_KEY,
            IntPreferenceValue(background.value or OPAQUE),
        )
        write(AnimePlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_OPACITY_KEY, FloatPreferenceValue(alpha / 255f))
    }
    convertTextEdge()
    (take("pref_sub_pos") as? IntPreferenceValue)?.let { height ->
        // Aniyomi places subtitles at a height from 0 at the top to 100 at the bottom, always centred (D068)
        val position = when (height.value) {
            in Int.MIN_VALUE..33 -> SubtitlePosition.TOP_CENTER
            in 34..66 -> SubtitlePosition.MIDDLE_CENTER
            else -> SubtitlePosition.BOTTOM_CENTER
        }
        write(AnimePlayerPreferences.DEFAULT_SUBTITLE_POSITION_KEY, StringPreferenceValue(position.name))
    }
}

/**
 * Aniyomi sizes subtitles by mpv's font size, scaled, which mpv measures against a 720-line picture; Katari by a share
 * of the picture's height (D066). Aniyomi's default size leaves Katari's default.
 */
private fun AniyomiSettings.convertTextSize() {
    val fontSize = take("pref_subtitles_font_size") as? IntPreferenceValue
    val scale = take("pref_sub_scale") as? FloatPreferenceValue
    if (fontSize == null && scale == null) return
    val size = (fontSize?.value ?: DEFAULT_FONT_SIZE) * (scale?.value ?: 1f)
    if (size == DEFAULT_FONT_SIZE.toFloat()) return
    val share = (size / MPV_REFERENCE_HEIGHT).coerceIn(
        AnimePlayerPreferences.MIN_SUBTITLE_TEXT_SIZE,
        AnimePlayerPreferences.MAX_SUBTITLE_TEXT_SIZE,
    )
    write(AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE_KEY, FloatPreferenceValue(share))
}

/**
 * Aniyomi's outline and shadow is Katari's outline, or its shadow where the outline is gone and the shadow kept. Its
 * boxes are a background, which Katari sets apart, so the text gets no edge and the box is listed (D067). The border
 * size and shadow offset stay listed: Katari draws its own.
 */
private fun AniyomiSettings.convertTextEdge() {
    val style = (take("pref_border_style_subtitles") as? StringPreferenceValue)?.value
    val borderSize = (peek("pref_sub_border_size") as? IntPreferenceValue)?.value
    val shadowOffset = (peek("sub_shadow_offset") as? IntPreferenceValue)?.value
    if (style == null && borderSize == null && shadowOffset == null) return
    val edge = when (style) {
        "OpaqueBox", "BackgroundBox" -> {
            leaveBehind(Item(BORDER_STYLE.nameIn(app), MR.strings.backup_conversion_aniyomi_subtitle_box))
            SubtitleTextEdge.NONE
        }
        else -> if (borderSize == 0 && (shadowOffset ?: 0) > 0) SubtitleTextEdge.SHADOW else SubtitleTextEdge.OUTLINE
    }
    write(AnimePlayerPreferences.DEFAULT_SUBTITLE_TEXT_EDGE_KEY, StringPreferenceValue(edge.name))
}

private const val OPAQUE = 0xFF000000.toInt()
private const val DEFAULT_FONT_SIZE = 55
private const val MPV_REFERENCE_HEIGHT = 720f

private val BORDER_STYLE = OtherAppSettingLabel(
    MR.strings.backup_conversion_aniyomi_player_sheets_sub_typography_border_style,
    listOf(
        MR.strings.backup_conversion_aniyomi_place_video_player,
        MR.strings.anime_playback_subtitles,
        MR.strings.backup_conversion_aniyomi_place_player_sheets_subtitles_settings_title,
        MR.strings.backup_conversion_aniyomi_place_player_sheets_sub_typography_title,
    ),
)
