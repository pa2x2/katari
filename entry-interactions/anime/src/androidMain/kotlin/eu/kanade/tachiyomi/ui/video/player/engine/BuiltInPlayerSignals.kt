package eu.kanade.tachiyomi.ui.video.player.engine

import eu.kanade.tachiyomi.ui.video.player.VideoPlayerSubtitleOption
import eu.kanade.tachiyomi.ui.video.player.tracks.VideoPlayerAudioTrackOption
import mihon.video.engine.spi.tracks.TrackKind
import mihon.video.engine.spi.tracks.VideoTracks

/**
 * What the built-in player's tracks say mpv would show better: styled (ASS/SSA) subtitles chosen to show, which the
 * built-in player draws as plain text, or a kind of track it can't decode at all, which it plays without. A kind it can
 * decode one track of plays, so a second, undecodable language doesn't count.
 */
internal fun VideoTracks.mpvSignal(): EngineSwitchReason? = when {
    groups.any { group -> group.kind == TrackKind.Text && group.tracks.any { it.selected && it.isStyledSubtitle } } ->
        EngineSwitchReason.StyledSubtitles
    undecodable(TrackKind.Video) -> EngineSwitchReason.UnsupportedVideo
    undecodable(TrackKind.Audio) -> EngineSwitchReason.UnsupportedAudio
    undecodable(TrackKind.Text) -> EngineSwitchReason.UnsupportedSubtitles
    else -> null
}

/**
 * The viewer's audio or subtitle pick, by its [audioKey] or [subtitleKey], is a track the built-in player lists but
 * can't decode, so only mpv plays it.
 */
internal fun pickedTrackSignal(
    audioKey: String?,
    audio: List<VideoPlayerAudioTrackOption>,
    subtitleKey: String?,
    subtitles: List<VideoPlayerSubtitleOption>,
): EngineSwitchReason? = when {
    audio.any { it.needsMpv && it.key == audioKey } -> EngineSwitchReason.UnsupportedAudio
    subtitles.any { it.needsMpv && it.key == subtitleKey } -> EngineSwitchReason.UnsupportedSubtitles
    else -> null
}

private fun VideoTracks.undecodable(kind: TrackKind): Boolean =
    containsKind(kind) && groups.none { it.kind == kind && it.supported }
