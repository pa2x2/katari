package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import mihon.video.engine.spi.tracks.subtitleChoiceKey

internal fun VideoStream.toMediaItem(
    subtitles: List<VideoSubtitle>,
    metadata: MediaMetadata = MediaMetadata.EMPTY,
): MediaItem {
    return MediaItem.Builder()
        .setUri(request.url)
        .setMediaMetadata(metadata)
        .setMimeType(mimeType ?: type.toMimeType())
        .setSubtitleConfigurations(subtitles.map(VideoSubtitle::toMediaItemSubtitle))
        .build()
}

internal fun VideoSubtitle.toMediaItemSubtitle(): MediaItem.SubtitleConfiguration {
    val selectionFlags = buildSelectionFlags(isDefault = isDefault, isForced = isForced)
    val roleFlags = C.ROLE_FLAG_SUBTITLE
    return MediaItem.SubtitleConfiguration.Builder(Uri.parse(request.url))
        .setMimeType(mimeType ?: inferSubtitleMimeType(request.url))
        .setLanguage(language)
        .setSelectionFlags(selectionFlags)
        .setRoleFlags(roleFlags)
        .setLabel(label)
        .setId(subtitleChoiceKey(this))
        .build()
}

private fun VideoStreamType.toMimeType(): String? {
    return when (this) {
        VideoStreamType.HLS -> MimeTypes.APPLICATION_M3U8
        VideoStreamType.DASH -> MimeTypes.APPLICATION_MPD
        VideoStreamType.PROGRESSIVE -> MimeTypes.VIDEO_MP4
        VideoStreamType.UNKNOWN -> null
    }
}

private fun inferSubtitleMimeType(url: String): String? {
    return when {
        url.endsWith(".vtt", ignoreCase = true) || url.endsWith(".webvtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
        url.endsWith(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
        url.endsWith(".ass", ignoreCase = true) || url.endsWith(".ssa", ignoreCase = true) -> MimeTypes.TEXT_SSA
        else -> null
    }
}

private fun buildSelectionFlags(isDefault: Boolean, isForced: Boolean): Int {
    var flags = 0
    if (isDefault) {
        flags = flags or C.SELECTION_FLAG_DEFAULT
    }
    if (isForced) {
        flags = flags or C.SELECTION_FLAG_FORCED
    }
    return flags
}
