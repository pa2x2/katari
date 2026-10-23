package eu.kanade.tachiyomi.ui.video.player.engine

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks

/**
 * What the built-in player's [Tracks] say mpv would show better: styled (ASS/SSA) subtitles chosen to show, which the
 * built-in player draws as plain text, or a kind of track it can't decode at all, which it plays without. A kind it can
 * decode one track of plays, so a second, undecodable language doesn't count.
 */
internal fun Tracks.mpvSignal(): EngineSwitchReason? = when {
    groups.any { it.type == C.TRACK_TYPE_TEXT && it.isSelected && it.selectedFormats().any { f -> f.isStyled() } } ->
        EngineSwitchReason.StyledSubtitles
    undecodable(C.TRACK_TYPE_VIDEO) -> EngineSwitchReason.UnsupportedVideo
    undecodable(C.TRACK_TYPE_AUDIO) -> EngineSwitchReason.UnsupportedAudio
    undecodable(C.TRACK_TYPE_TEXT) -> EngineSwitchReason.UnsupportedSubtitles
    else -> null
}

private fun Tracks.undecodable(type: Int): Boolean =
    containsType(type) && groups.none { it.type == type && it.isSupported }

private fun Tracks.Group.selectedFormats(): List<Format> = (0 until length).filter(
    ::isTrackSelected,
).map(::getTrackFormat)

// Subtitles parsed while extracting report Media3's cue format, with the original format in codecs.
private fun Format.isStyled(): Boolean =
    sampleMimeType == MimeTypes.TEXT_SSA || codecs == MimeTypes.TEXT_SSA
