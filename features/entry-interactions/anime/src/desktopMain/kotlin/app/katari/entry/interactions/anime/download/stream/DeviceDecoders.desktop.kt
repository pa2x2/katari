package app.katari.entry.interactions.anime.download.stream

// The desktop player is to decode in software, which plays nearly every codec, so no rendition is passed over.
internal actual fun isDecodableOnDevice(codecs: String?): Boolean = true
