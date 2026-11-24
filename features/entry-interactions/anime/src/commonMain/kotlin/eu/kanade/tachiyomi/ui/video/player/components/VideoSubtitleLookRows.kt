package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.entry.interactions.settings.AnimePlayerPreferences.Companion.BUILT_IN_SUBTITLE_TEXT_SIZE
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.list.ChipsRow
import app.katari.presentation.kit.list.ColorChoiceRow
import app.katari.presentation.kit.list.PositionChoiceRow
import app.katari.presentation.kit.list.SliderRow
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.video.player.subtitle.MAX_SUBTITLE_TEXT_SIZE
import eu.kanade.tachiyomi.ui.video.player.subtitle.MIN_SUBTITLE_TEXT_SIZE
import eu.kanade.tachiyomi.ui.video.player.subtitle.SUBTITLE_BACKGROUND_COLOR_PRESETS
import eu.kanade.tachiyomi.ui.video.player.subtitle.SUBTITLE_TEXT_COLOR_PRESETS
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import tachiyomi.domain.entry.model.SubtitlePosition
import tachiyomi.domain.entry.model.SubtitleTextEdge
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * The rows that set a subtitle look, for the player's subtitle editor and the default look's Settings page alike. Where
 * subtitles rest is picked from a grid, which shows no choice while they're dragged off it, and Recenter puts them back
 * at the bottom centre. Text size is shown as a share of the built-in size and background opacity as a percentage;
 * both slide in whole percent steps.
 */
@Composable
internal fun VideoSubtitleLookRows(appearance: VideoSubtitleAppearance, onChange: (VideoSubtitleAppearance) -> Unit) {
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    val textSizePercent = appearance.textSize.toTextSizePercent()
    val backgroundOpacityPercent = (appearance.backgroundOpacity * 100f).roundToInt()

    PositionChoiceRow(
        title = stringResource(MR.strings.anime_playback_subtitle_position),
        placeLabels = SubtitlePosition.entries.map { stringResource(it.label) },
        selected = appearance.position.ordinal.takeIf { appearance.restsInPlace },
        onSelect = { onChange(appearance.restingIn(SubtitlePosition.entries[it])) },
        actionLabel = stringResource(MR.strings.anime_playback_subtitle_recenter),
        onAction = { onChange(appearance.restingIn(SubtitlePosition.BOTTOM_CENTER)) },
    )
    SliderRow(
        title = stringResource(MR.strings.anime_playback_subtitle_size),
        value = textSizePercent,
        valueRange = TEXT_SIZE_PERCENT_RANGE,
        onValueChange = { onChange(appearance.copy(textSize = it.toTextSize())) },
        valueText = percentFormat.format(textSizePercent / 100f),
        steps = 0,
    )
    ColorChoiceRow(
        title = stringResource(MR.strings.anime_playback_subtitle_text_color),
        colors = SUBTITLE_TEXT_COLOR_PRESETS,
        selected = appearance.textColor,
        onSelect = { onChange(appearance.copy(textColor = it)) },
    )
    ColorChoiceRow(
        title = stringResource(MR.strings.anime_playback_subtitle_background_color),
        colors = SUBTITLE_BACKGROUND_COLOR_PRESETS,
        selected = appearance.backgroundColor,
        onSelect = { onChange(appearance.copy(backgroundColor = it)) },
    )
    ChipsRow(title = stringResource(MR.strings.anime_playback_subtitle_text_edge)) {
        SubtitleTextEdge.entries.forEach { edge ->
            FilterChip(
                label = stringResource(edge.label),
                selected = appearance.textEdge == edge,
                onClick = { onChange(appearance.copy(textEdge = edge)) },
            )
        }
    }
    SliderRow(
        title = stringResource(MR.strings.anime_playback_subtitle_background_opacity),
        value = backgroundOpacityPercent,
        valueRange = 0..100,
        onValueChange = { onChange(appearance.copy(backgroundOpacity = it / 100f)) },
        valueText = percentFormat.format(backgroundOpacityPercent / 100f),
        steps = 0,
    )
}

private val SubtitlePosition.label: StringResource
    get() = when (this) {
        SubtitlePosition.TOP_LEFT -> MR.strings.anime_playback_subtitle_position_top_left
        SubtitlePosition.TOP_CENTER -> MR.strings.anime_playback_subtitle_position_top_center
        SubtitlePosition.TOP_RIGHT -> MR.strings.anime_playback_subtitle_position_top_right
        SubtitlePosition.MIDDLE_LEFT -> MR.strings.anime_playback_subtitle_position_middle_left
        SubtitlePosition.MIDDLE_CENTER -> MR.strings.anime_playback_subtitle_position_middle_center
        SubtitlePosition.MIDDLE_RIGHT -> MR.strings.anime_playback_subtitle_position_middle_right
        SubtitlePosition.BOTTOM_LEFT -> MR.strings.anime_playback_subtitle_position_bottom_left
        SubtitlePosition.BOTTOM_CENTER -> MR.strings.anime_playback_subtitle_position_bottom_center
        SubtitlePosition.BOTTOM_RIGHT -> MR.strings.anime_playback_subtitle_position_bottom_right
    }

private val SubtitleTextEdge.label: StringResource
    get() = when (this) {
        SubtitleTextEdge.NONE -> MR.strings.anime_playback_subtitle_text_edge_none
        SubtitleTextEdge.OUTLINE -> MR.strings.anime_playback_subtitle_text_edge_outline
        SubtitleTextEdge.SHADOW -> MR.strings.anime_playback_subtitle_text_edge_shadow
    }

private fun Float.toTextSizePercent(): Int = (this / BUILT_IN_SUBTITLE_TEXT_SIZE * 100f).roundToInt()

private fun Int.toTextSize(): Float = this / 100f * BUILT_IN_SUBTITLE_TEXT_SIZE

private val TEXT_SIZE_PERCENT_RANGE =
    MIN_SUBTITLE_TEXT_SIZE.toTextSizePercent()..MAX_SUBTITLE_TEXT_SIZE.toTextSizePercent()
