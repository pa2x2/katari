package eu.kanade.tachiyomi.ui.video.player.tracks

import app.katari.video.engine.spi.tracks.TrackKind
import app.katari.video.engine.spi.tracks.VideoTrack
import app.katari.video.engine.spi.tracks.VideoTrackGroup
import app.katari.video.engine.spi.tracks.VideoTracks
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleSelection
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/*
 * The keys a subtitle or audio pick is found by again, in the next episode, the other engine or a saved choice, come
 * from the tracks engines publish. Tracks are laid out as ExoPlayer publishes a stream with one external subtitle and
 * one external audio track merged in.
 */
class VideoPlayerTrackKeysTest {

    private val subtitle = VideoSubtitle(VideoRequest("https://cdn.example.com/en.vtt"), label = "English", key = "en")
    private val dub = VideoAudioTrack(
        VideoRequest("https://cdn.example.com/en.m4a"),
        label = "English",
        language = "en",
    )

    private val tracks = VideoTracks(
        listOf(
            group(TrackKind.Video, track("0:0:1", height = 720), track("0:0:2", height = 1080)),
            group(TrackKind.Audio, track("0:0:3", language = "ja")),
            group(TrackKind.Audio, track("0:0:4", language = "ja")),
            group(TrackKind.Audio, track("1:0", language = "und")),
            group(TrackKind.Text, track("0:0:5", label = "Signs", language = "ENG", isDefault = true)),
            group(TrackKind.Text, track("0:1:en", label = "English")),
        ),
    )

    @Test
    fun `subtitle keys match saved and legacy picks, and external tracks map to the stream's own`() {
        tracks.availableSubtitleTracks(listOf(subtitle)).map { it.key } shouldBe
            listOf("off", "en", "embedded:signs|eng|true|false")

        // A pick saved by group and track position before fingerprints is found by them, then keyed anew.
        val legacy = VideoPlayerSubtitleSelection.Embedded(
            groupIndex = 4,
            trackIndex = 0,
            key = "embedded:4:0",
            label = "Signs",
            language = null,
            isDefault = false,
            isForced = false,
        )
        tracks.resolveAppliedSubtitleSelection(legacy, listOf(subtitle)) shouldBe
            VideoPlayerSubtitleSelection.Embedded(
                groupIndex = 4,
                trackIndex = 0,
                key = "embedded:signs|eng|true|false",
                label = "Signs (Default)",
                language = "ENG",
                isDefault = true,
                isForced = false,
            )

        val audio = tracks.availableAudioTracks(listOf(dub))
        audio.map { it.key } shouldBe listOf("ja|", "ja|#1", "en|English")
        audio.last().label shouldBe "English"
    }

    private fun group(kind: TrackKind, vararg tracks: VideoTrack) = VideoTrackGroup(
        kind = kind,
        id = tracks.first().id.orEmpty(),
        adaptive = tracks.size > 1,
        tracks = tracks.toList(),
    )

    private fun track(
        id: String,
        label: String? = null,
        language: String? = null,
        isDefault: Boolean = false,
        height: Int? = null,
    ) = VideoTrack(
        id = id,
        label = label,
        language = language,
        isDefault = isDefault,
        isForced = false,
        mimeType = null,
        codecs = null,
        height = height,
        supported = true,
        selected = false,
    )
}
