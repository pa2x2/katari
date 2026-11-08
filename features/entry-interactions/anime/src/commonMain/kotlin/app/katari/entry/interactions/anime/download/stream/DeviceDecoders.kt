package app.katari.entry.interactions.anime.download.stream

/**
 * Whether this device has a decoder for every codec in [codecs], an RFC 6381 list such as `avc1.64001f,mp4a.40.2`.
 * Text, and a codec the platform can't name, count as decodable: only media known to fail in the built-in player is
 * passed over.
 */
internal expect fun isDecodableOnDevice(codecs: String?): Boolean
