package mihon.entry.interactions.anime.download.stream.segments

import okhttp3.HttpUrl
import java.security.MessageDigest

/** A file of a segmented download: all of [url], or only the bytes [range] of it, stored as [name]. */
internal class SegmentFile(val name: String, val url: HttpUrl, val range: LongRange?)

/**
 * What the segmented download of one stream writes: the [files], then the local playlists or manifest that name
 * them, by name in [texts]. [part] is the name of the root one, which tells the stream from another in the folder.
 */
internal class SegmentPlan(val part: String, val texts: Map<String, String>, val files: List<SegmentFile>) {
    /**
     * Identifies what the plan stores rather than where it fetches it from: the local texts, which hold the variant or
     * representations and every segment's place and timing, and each file's name and byte range. Two fetches of an
     * unchanged stream agree though their links differ; a different variant, segment count or timing doesn't.
     */
    val id: String by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        fun add(value: String) = digest.update((value + "\u0000").toByteArray())
        add(part)
        texts.forEach { (name, text) ->
            add(name)
            add(text)
        }
        files.forEach { file ->
            add(file.name)
            add(file.range?.let { "${it.first}-${it.last}" }.orEmpty())
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    }
}
