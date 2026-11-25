package eu.kanade.tachiyomi.ui.video.player.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import app.katari.entry.interactions.settings.AnimePlayerPreferences
import app.katari.entry.interactions.settings.DefaultSubtitleLookPage
import app.katari.presentation.kit.content.VideoFrameSample
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.RowListHeader
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.list.ToggleRow
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.video.engine.spi.player.SubtitleCue
import eu.kanade.tachiyomi.ui.video.player.components.VideoSubtitleLookRows
import eu.kanade.tachiyomi.ui.video.player.subtitle.VideoSubtitles
import eu.kanade.tachiyomi.ui.video.player.subtitle.setSubtitleDefault
import eu.kanade.tachiyomi.ui.video.player.subtitle.subtitleDefault
import eu.kanade.tachiyomi.ui.video.player.subtitle.videoSubtitleLayout
import tachiyomi.domain.entry.model.PictureFraming
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

/**
 * The default subtitle look's Settings page: a sample picture with the look over it, which stays in view while the
 * subtitle editor's rows under it change the look. Each change is saved at once, as other settings are, and the
 * player's subtitles are drawn over the sample, so it shows them as the player will.
 */
internal class VideoSubtitleDefaultLookPage(private val preferences: AnimePlayerPreferences) : DefaultSubtitleLookPage {
    @Composable
    override fun Content(navigateUp: () -> Unit) {
        // Held while the page is open, so a slider follows the finger rather than the stored value catching up
        var look by remember { mutableStateOf(preferences.subtitleDefault()) }
        val keepInPicture by preferences.subtitlesInPicture.collectAsState()
        val sample = stringResource(MR.strings.anime_playback_subtitle_sample)
        val cues = remember(sample) { listOf(SubtitleCue(sample)) }
        val spacing = KatariTheme.spacing
        val gapPx = with(LocalDensity.current) { spacing.s.toPx() }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_anime_default_subtitle_look),
                    navigateUp = navigateUp,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
                RowListHeader {
                    VideoFrameSample(modifier = Modifier.padding(horizontal = spacing.m, vertical = spacing.s)) {
                        VideoSubtitles(
                            cues = cues,
                            appearance = look,
                            layout = { width, height ->
                                videoSubtitleLayout(
                                    playerWidth = width,
                                    playerHeight = height,
                                    aspectRatio = null,
                                    framing = PictureFraming.Fit,
                                    appearance = look,
                                    keepInPicture = keepInPicture,
                                    gapPx = gapPx,
                                )
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                RowList(contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding())) {
                    item { RowGroupNote(stringResource(MR.strings.pref_anime_default_subtitle_look_note)) }
                    item {
                        RowGroupItem(RowGroupPosition.Alone) {
                            Column {
                                VideoSubtitleLookRows(appearance = look) {
                                    look = it.asDefault()
                                    preferences.setSubtitleDefault(look)
                                }
                            }
                        }
                    }
                    item { RowGroupGap() }
                    item {
                        RowGroupItem(RowGroupPosition.Alone) {
                            ToggleRow(
                                title = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture),
                                subtitle = stringResource(MR.strings.anime_playback_subtitle_keep_in_picture_summary),
                                checked = keepInPicture,
                                onCheckedChange = preferences.subtitlesInPicture::set,
                            )
                        }
                    }
                }
            }
        }
    }
}
