package mihon.entry.interactions.anime.download.stream

internal fun mediaFileExtension(url: String, mimeType: String?, fallback: String): String {
    val mimeExtension = when {
        mimeType?.contains("matroska", ignoreCase = true) == true -> "mkv"
        mimeType?.contains("mp2t", ignoreCase = true) == true -> "ts"
        mimeType?.contains("x-flv", ignoreCase = true) == true -> "flv"
        mimeType?.contains("msvideo", ignoreCase = true) == true -> "avi"
        mimeType?.contains("x-ms-asf", ignoreCase = true) == true -> "asf"
        mimeType?.contains("ogg", ignoreCase = true) == true -> "ogg"
        mimeType?.equals("audio/mpeg", ignoreCase = true) == true -> "mp3"
        mimeType?.equals("audio/aac", ignoreCase = true) == true -> "aac"
        mimeType?.contains("mp4", ignoreCase = true) == true -> "mp4"
        mimeType?.contains("webm", ignoreCase = true) == true -> "webm"
        mimeType?.contains("mpeg", ignoreCase = true) == true -> "mp4"
        mimeType?.contains("vtt", ignoreCase = true) == true -> "vtt"
        mimeType?.contains("srt", ignoreCase = true) == true -> "srt"
        mimeType?.contains("ass", ignoreCase = true) == true -> "ass"
        else -> null
    }
    if (mimeExtension != null) return mimeExtension

    val path = url.substringBefore('?').substringBefore('#')
    val urlExtension = path.substringAfterLast('.', missingDelimiterValue = "")
    return urlExtension.takeIf { it.isNotBlank() && it.length <= 5 } ?: fallback
}
