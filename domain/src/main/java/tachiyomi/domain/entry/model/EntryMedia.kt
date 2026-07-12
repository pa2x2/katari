package tachiyomi.domain.entry.model

sealed class EntryMedia {
    data class ImagePages(val pages: List<EntryPage>) : EntryMedia()

    data class Playback(val descriptor: PlaybackDescriptor) : EntryMedia()
}

data class EntryPage(
    val index: Int,
    val url: String,
    val imageUrl: String? = null,
)

data class PlaybackDescriptor(
    val videoUrl: String,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
)

data class SubtitleTrack(
    val url: String,
    val language: String,
    val label: String? = null,
)
