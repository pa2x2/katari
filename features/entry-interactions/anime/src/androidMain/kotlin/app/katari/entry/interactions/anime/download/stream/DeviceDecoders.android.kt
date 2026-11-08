package app.katari.entry.interactions.anime.download.stream

import android.media.MediaCodecList
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

@OptIn(markerClass = [UnstableApi::class])
internal actual fun isDecodableOnDevice(codecs: String?): Boolean =
    codecs.orEmpty().split(',').map(String::trim).filter(String::isNotEmpty).all { codec ->
        val mimeType = MimeTypes.getMediaMimeType(codec) ?: return@all true
        if (!MimeTypes.isVideo(mimeType) && !MimeTypes.isAudio(mimeType)) return@all true
        mimeType.lowercase() in decoderMimeTypes
    }

private val decoderMimeTypes: Set<String> by lazy {
    MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
        .filterNot { it.isEncoder }
        .flatMapTo(HashSet()) { info -> info.supportedTypes.map(String::lowercase) }
}
