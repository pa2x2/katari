package eu.kanade.tachiyomi.ui.video.player.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import app.katari.entry.interactions.anime.introtimes.IntroTimesService
import app.katari.entry.interactions.settings.AnimeMarkerSkipMode
import app.katari.entry.viewer.settings.ViewerSettingBinding
import app.katari.video.engine.spi.player.VideoPlayer
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerViewModel
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerAniSkipMatchPage
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerEngineChoice
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerIntroTimesRows
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerMarkerSkipChoice
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerMarkerSkipRow
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettings
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsContent
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerSettingsSection
import eu.kanade.tachiyomi.ui.video.player.components.VideoPlayerTheIntroDbMatchPage
import eu.kanade.tachiyomi.ui.video.player.engine.MpvAvailability
import eu.kanade.tachiyomi.ui.video.player.engine.mpvTrackPick
import eu.kanade.tachiyomi.ui.video.player.engine.pickedTrackSignal
import eu.kanade.tachiyomi.ui.video.player.introtimes.VideoPlayerIntroTimesState
import eu.kanade.tachiyomi.ui.video.player.introtimes.open
import eu.kanade.tachiyomi.ui.video.player.markers.VideoPlayerChapter
import eu.kanade.tachiyomi.ui.video.player.markers.chapterAt
import eu.kanade.tachiyomi.ui.video.player.markers.skippable
import eu.kanade.tachiyomi.ui.video.player.subtitleSelectionKey
import kotlinx.coroutines.launch

/**
 * The player settings: speed, picture, quality, subtitles, audio, source, the episode's chapters, and the title's skip
 * mode and player engine. Picking a chapter seeks to it through [onSeek] and closes the settings to show it playing.
 * Applying a source or resetting saves the progress first; the subtitle settings close the settings, pause the video
 * and open the subtitle editor, while adjusting the subtitle timing closes them for the timing bar over the video as it
 * plays. A track only mpv plays moves Auto to mpv when picked. When [introTimes] are looked up online, the skip section
 * says where they stand with each service and opens the page that changes a service's match in place of the rest.
 */
@Composable
internal fun VideoPlayerSettingsLayer(
    current: VideoPlayerViewModel.State.Ready,
    ui: VideoPlayerWatchUiState,
    host: VideoPlayerScreenHost,
    viewModel: VideoPlayerViewModel,
    actions: VideoPlayerWatchActions,
    currentPlayer: VideoPlayer,
    settingsSection: VideoPlayerSettingsSection?,
    isInPictureInPictureMode: Boolean,
    engine: WatchedPlayerEngine,
    markerSkipSetting: ViewerSettingBinding<AnimeMarkerSkipMode>?,
    markers: List<PlaybackMarker>,
    introTimes: VideoPlayerIntroTimesState?,
    chapters: List<VideoPlayerChapter>,
    onSeek: (Long) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val subtitleTiming by viewModel.subtitleTiming.state.collectAsState()
    val markerSkipOverride = markerSkipSetting?.let { it.state.collectAsState().value.entryOverride }
    val showsMarkerSkip = markerSkipSetting != null && (introTimes != null || markers.any { it.skippable })
    // The service whose match page shows in place of the settings, if any.
    var matchPage by remember { mutableStateOf<IntroTimesService?>(null) }
    LaunchedEffect(ui.settingsVisible, introTimes?.episode) {
        if (!ui.settingsVisible || introTimes?.episode == null) matchPage = null
    }
    VideoPlayerSettings(
        visible = ui.settingsVisible && !isInPictureInPictureMode && !ui.subtitleEditorVisible,
        onDismissRequest = actions.dismissSettings,
    ) { settingsFrame, settingsModifier ->
        if (introTimes?.episode != null) {
            when (matchPage) {
                IntroTimesService.ANISKIP -> {
                    VideoPlayerAniSkipMatchPage(
                        frame = settingsFrame,
                        match = viewModel.aniSkipMatch,
                        onUse = { series ->
                            viewModel.introTimes.pickAniSkipSeries(series)
                            matchPage = null
                        },
                        onBack = { matchPage = null },
                        onDismissRequest = actions.dismissSettings,
                        modifier = settingsModifier,
                    )
                    return@VideoPlayerSettings
                }
                IntroTimesService.THEINTRODB -> {
                    VideoPlayerTheIntroDbMatchPage(
                        frame = settingsFrame,
                        match = viewModel.theIntroDbMatch,
                        onUse = { media ->
                            viewModel.introTimes.pickTheIntroDbMedia(media)
                            matchPage = null
                        },
                        onBack = { matchPage = null },
                        onDismissRequest = actions.dismissSettings,
                        modifier = settingsModifier,
                    )
                    return@VideoPlayerSettings
                }
                null -> Unit
            }
        }
        VideoPlayerSettingsContent(
            frame = settingsFrame,
            playback = current.playback,
            openingEpisodeName = (current.episodeChange as? VideoPlayerViewModel.EpisodeChange.Loading)?.episodeName,
            initialSection = settingsSection,
            onDismissRequest = actions.dismissSettings,
            onSelectSpeed = { speed ->
                actions.endSpeedBoost()
                viewModel.updateSessionPlaybackSpeed(speed)
            },
            onSelectPicture = viewModel::setPictureFraming,
            onSelectAdaptiveQuality = viewModel::selectAdaptiveQuality,
            onSelectSubtitle = { selection ->
                viewModel.selectSubtitle(selection)
                pickedTrackSignal(
                    audioKey = null,
                    audio = emptyList(),
                    subtitleKey = subtitleSelectionKey(selection),
                    subtitles = current.playback.subtitleOptions,
                )?.let { engine.onTrackPicked(it, currentPlayer.currentPositionMs) }
            },
            onSelectAudioTrack = { key ->
                viewModel.selectAudioTrack(key)
                pickedTrackSignal(
                    audioKey = key,
                    audio = current.playback.audioTracks,
                    subtitleKey = null,
                    subtitles = emptyList(),
                )?.let { engine.onTrackPicked(it, currentPlayer.currentPositionMs) }
            },
            onApplySource = { selection, subtitle ->
                host.playback.persist(currentPlayer)
                // Selected first: applying the source carries the current subtitle over.
                subtitle?.let(viewModel::selectSubtitle)
                viewModel.applySourceSelection(selection)
            },
            onOpenDefaultSettings = {
                actions.dismissSettings()
                host.onOpenDefaultSettings()
            },
            onReset = {
                actions.endSpeedBoost()
                host.playback.persist(currentPlayer)
                viewModel.resetPlayerSettings()
            },
            onPreviewSourceSelection = viewModel::previewSourceSelection,
            onOpenSubtitleSettings = {
                actions.endSpeedBoost()
                ui.settingsVisible = false
                ui.episodesDrawerVisible = false
                ui.subtitleEditorDraft = current.playback.subtitleAppearance
                ui.subtitleEditorKeepsInPicture = host.visit.subtitlesInPicture
                ui.resumePlaybackAfterSubtitleEditor = currentPlayer.state.value.isPlaying
                currentPlayer.pause()
                ui.subtitleEditorVisible = true
            },
            subtitleTimingDelayMs = subtitleTiming.delayMs.takeIf { subtitleTiming.adjustable },
            onAdjustSubtitleTiming = {
                actions.endSpeedBoost()
                ui.settingsVisible = false
                ui.subtitleTimingVisible = true
            },
            engine = VideoPlayerEngineChoice(
                override = current.playback.engineOverride,
                autoReason = engine.run.reason,
                mpvUsable = engine.mpv == MpvAvailability.Available,
                trackPick = mpvTrackPick(engine.run, current.playback.engineOverride, engine.mpv),
            ).takeIf { engine.mpv != MpvAvailability.Absent },
            onSelectEngine = { choice -> engine.choose(choice, currentPlayer.currentPositionMs) },
            chapters = chapters,
            currentChapter = chapters.chapterAt(ui.playbackSnapshot.positionMs),
            onSelectChapter = { chapter ->
                onSeek(chapter.startMs)
                actions.dismissSettings()
            },
            markerSkip = if (showsMarkerSkip) {
                {
                    VideoPlayerMarkerSkipRow(
                        choice = VideoPlayerMarkerSkipChoice(markerSkipOverride),
                        onSelect = { mode ->
                            scope.launch {
                                if (mode == null) {
                                    markerSkipSetting.clearEntryOverride()
                                } else {
                                    markerSkipSetting.setEntryOverride(mode)
                                }
                            }
                        },
                    )
                    if (introTimes != null) {
                        VideoPlayerIntroTimesRows(
                            state = introTimes,
                            onChange = { service ->
                                when (service) {
                                    IntroTimesService.ANISKIP ->
                                        viewModel.aniSkipMatch.open(introTimes, introTimes.aniSkip)
                                    IntroTimesService.THEINTRODB ->
                                        viewModel.theIntroDbMatch.open(introTimes, introTimes.theIntroDb)
                                }
                                matchPage = service
                            },
                            onRetry = viewModel.introTimes::retry,
                        )
                    }
                }
            } else {
                null
            },
            modifier = settingsModifier,
        )
    }
}
