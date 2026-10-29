package mihon.entry.interactions.anime.download.stream

import android.media.MediaCodecList
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi

/**
 * Whether this device has a decoder for every codec in [codecs], an RFC 6381 list such as `avc1.64001f,mp4a.40.2`.
 * Text, and a codec Media3 can't name, count as decodable: only media known to fail in the built-in player is passed
 * over.
 */
@OptIn(markerClass = [UnstableApi::class])
internal fun isDecodableOnDevice(codecs: String?): Boolean =
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
