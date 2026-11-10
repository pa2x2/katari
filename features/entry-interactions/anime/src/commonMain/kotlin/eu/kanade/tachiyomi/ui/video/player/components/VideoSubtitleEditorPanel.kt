package eu.kanade.tachiyomi.ui.video.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowLeft
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.chip.FilterChip
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.ChipsRow
import app.katari.presentation.kit.list.ColorChoiceRow
import app.katari.presentation.kit.list.InfoRow
import app.katari.presentation.kit.list.PlainRows
import app.katari.presentation.kit.list.PositionChoiceRow
import app.katari.presentation.kit.list.ProvideRowDensity
import app.katari.presentation.kit.list.RowDensity
import app.katari.presentation.kit.list.SliderRow
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.surface.SidePanel
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.ui.video.player.subtitle.DEFAULT_SUBTITLE_TEXT_SIZE
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
 * The subtitle style controls docked beside the video while the subtitle editor is open. Text size is shown as a share
 * of the default size and background opacity as a percentage; both slide in whole percent steps. When
 * [subtitleKeepsOwnStyle], the selected subtitle keeps its own styling, which these settings don't reach, and the panel
 * says so. Where they rest is picked from a grid, which shows no choice while they're dragged off it, and Recenter puts
 * them back at the bottom centre; [keepInPicture] stops subtitles resting at the bottom from moving into the band
 * under the picture.
 */
@Composable
internal fun VideoSubtitleEditorPanel(
    appearance: VideoSubtitleAppearance,
    subtitleKeepsOwnStyle: Boolean,
    keepInPicture: Boolean,
    onDraftChange: (VideoSubtitleAppearance) -> Unit,
    onKeepInPictureChange: (Boolean) -> Unit,
    onReset: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    val textSizePercent = appearance.textSize.toTextSizePercent()
    val backgroundOpacityPercent = (appearance.backgroundOpacity * 100f).roundToInt()

    SidePanel(modifier = modifier) {
        PlainRows {
            ProvideRowDensity(RowDensity.Compact) {
                Column(modifier = Modifier.fillMaxSize().padding(vertical = spacing.l)) {
                    Text(
                        text = stringResource(MR.strings.anime_playback_subtitle_settings),
                        modifier = Modifier.padding(horizontal = spacing.rowInset),
                        role = TextRole.SectionTitle,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (subtitleKeepsOwnStyle) {
                            InfoRow(stringResource(MR.strings.anime_playback_subtitle_own_style))
                        }
                        PositionChoiceRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_position),
                            placeLabels = SubtitlePosition.entries.map { stringResource(it.label) },
                            selected = appearance.position.ordinal.takeIf { appearance.restsInPlace },
                            onSelect = { onDraftChange(appearance.restingIn(SubtitlePosition.entries[it])) },
                            actionLabel = stringResource(MR.strings.anime_playback_subtitle_recenter),
                            onAction = { onDraftChange(appearance.restingIn(SubtitlePosition.BOTTOM_CENTER)) },
                        )
                        ToggleRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture),
                            subtitle = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture_summary),
                            checked = keepInPicture,
                            onCheckedChange = onKeepInPictureChange,
                        )
                        SliderRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_size),
                            value = textSizePercent,
                            valueRange = TEXT_SIZE_PERCENT_RANGE,
                            onValueChange = { onDraftChange(appearance.copy(textSize = it.toTextSize())) },
                            valueText = percentFormat.format(textSizePercent / 100f),
                            steps = 0,
                        )
                        ColorChoiceRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_text_color),
                            colors = SUBTITLE_TEXT_COLOR_PRESETS,
                            selected = appearance.textColor,
                            onSelect = { onDraftChange(appearance.copy(textColor = it)) },
                        )
                        ColorChoiceRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_background_color),
                            colors = SUBTITLE_BACKGROUND_COLOR_PRESETS,
                            selected = appearance.backgroundColor,
                            onSelect = { onDraftChange(appearance.copy(backgroundColor = it)) },
                        )
                        ChipsRow(title = stringResource(MR.strings.anime_playback_subtitle_text_edge)) {
                            SubtitleTextEdge.entries.forEach { edge ->
                                FilterChip(
                                    label = stringResource(edge.label),
                                    selected = appearance.textEdge == edge,
                                    onClick = { onDraftChange(appearance.copy(textEdge = edge)) },
                                )
                            }
                        }
                        SliderRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_background_opacity),
                            value = backgroundOpacityPercent,
                            valueRange = 0..100,
                            onValueChange = { onDraftChange(appearance.copy(backgroundOpacity = it / 100f)) },
                            valueText = percentFormat.format(backgroundOpacityPercent / 100f),
                            steps = 0,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = spacing.l, top = spacing.m, end = spacing.l),
                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Button(
                                label = stringResource(MR.strings.action_reset),
                                onClick = onReset,
                                style = ButtonStyle.Text,
                            )
                            Button(
                                label = stringResource(MR.strings.action_cancel),
                                onClick = onCancel,
                                style = ButtonStyle.Text,
                            )
                        }
                        Button(
                            label = stringResource(MR.strings.action_ok),
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** The tab beside the panel that slides it out of the way, so the whole video can be seen while placing subtitles. */
@Composable
internal fun VideoSubtitleEditorPanelHandle(
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OverlayPill(modifier = modifier, style = OverlayStyle.Raised, onClick = onToggle) {
        Icon(
            imageVector = if (expanded) {
                MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight
            } else {
                MaterialSymbols.AutoMirroredRounded.KeyboardArrowLeft
            },
            contentDescription = stringResource(MR.strings.anime_playback_subtitle_settings),
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.s, vertical = KatariTheme.spacing.xxl),
        )
    }
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

private fun Float.toTextSizePercent(): Int = (this / DEFAULT_SUBTITLE_TEXT_SIZE * 100f).roundToInt()

private fun Int.toTextSize(): Float = this / 100f * DEFAULT_SUBTITLE_TEXT_SIZE

private val TEXT_SIZE_PERCENT_RANGE =
    MIN_SUBTITLE_TEXT_SIZE.toTextSizePercent()..MAX_SUBTITLE_TEXT_SIZE.toTextSizePercent()
