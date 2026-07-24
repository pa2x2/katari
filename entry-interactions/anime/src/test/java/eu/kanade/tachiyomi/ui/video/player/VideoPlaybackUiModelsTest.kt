package eu.kanade.tachiyomi.ui.video.player

import eu.kanade.tachiyomi.source.entry.PlaybackDescriptor
import eu.kanade.tachiyomi.source.entry.PlaybackSelection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VideoPlaybackUiModelsTest {

    @Test
    fun `player settings defaults clear selections and restore automatic playback`() {
        assertEquals(
            VideoPlayerSettingsDraft(
                sourceSelection = PlaybackSelection(),
                adaptiveQuality = VideoAdaptiveQualityPreference.Auto,
                playbackSpeed = DEFAULT_PLAYER_SETTINGS_PLAYBACK_SPEED,
                subtitleSelection = VideoPlayerSubtitleSelection.Default,
                resetToDefaults = true,
            ),
            defaultVideoPlayerSettingsDraft(),
        )
    }

    @Test
    fun `reset settings display the source resolved defaults without persisting them`() {
        val resetDraft = defaultVideoPlayerSettingsDraft()
        val resolvedDefaults = PlaybackSelection(
            dubKey = "default-dub",
            streamKey = "default-stream",
            sourceQualityKey = "default-quality",
        )

        assertEquals(
            resolvedDefaults,
            resetDraft.sourceSelectionForDisplay(
                VideoPlaybackPreviewState(
                    selection = PlaybackSelection(),
                    playbackData = PlaybackDescriptor(selection = resolvedDefaults),
                ),
            ),
        )
        assertEquals(PlaybackSelection(), resetDraft.sourceSelection)
    }
}
