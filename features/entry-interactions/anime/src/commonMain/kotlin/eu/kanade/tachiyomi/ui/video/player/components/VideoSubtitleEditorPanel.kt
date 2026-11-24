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
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowLeft
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.InfoRow
import app.katari.presentation.kit.list.PlainRows
import app.katari.presentation.kit.list.ProvideRowDensity
import app.katari.presentation.kit.list.RowDensity
import app.katari.presentation.kit.list.RowGroupSubheader
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.surface.OverlayPill
import app.katari.presentation.kit.surface.OverlayStyle
import app.katari.presentation.kit.surface.SidePanel
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitleAppearance
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The subtitle style controls docked beside the video while the subtitle editor is open. When [subtitleKeepsOwnStyle],
 * the selected subtitle keeps its own styling, which these settings don't reach, and the panel says so.
 *
 * The look is the series' own, while the last part of the panel is for every series: [keepInPicture] stops subtitles
 * resting at the bottom from moving into the band under the picture.
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
                        VideoSubtitleLookRows(appearance = appearance, onChange = onDraftChange)
                        RowGroupSubheader(stringResource(MR.strings.anime_playback_subtitle_all_series))
                        ToggleRow(
                            title = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture),
                            subtitle = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture_summary),
                            checked = keepInPicture,
                            onCheckedChange = onKeepInPictureChange,
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
