package app.katari.showcase.extension.catalog

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Mirrors showcase/content/catalog.ts and resolved-work.ts; fields the extension does not use are ignored.

internal val showcaseJson = Json { ignoreUnknownKeys = true }

@Serializable
internal enum class ShowcaseEntryType(val entryType: EntryType) {
    @SerialName("manga")
    MANGA(EntryType.MANGA),

    @SerialName("book")
    BOOK(EntryType.BOOK),

    @SerialName("anime")
    ANIME(EntryType.ANIME),
}

@Serializable
internal data class ShowcaseSourceDefinition(
    val id: String,
    val name: String,
    val language: String,
    val type: ShowcaseEntryType,
)

@Serializable
internal data class ShowcaseCatalog(
    val works: List<ShowcaseWork>,
)

@Serializable
internal data class ShowcaseWork(
    val id: String,
    val source: String,
    val type: ShowcaseEntryType,
    val title: String,
    val author: String,
    val artist: String? = null,
    val description: String,
    val genres: List<String>,
    val status: String,
    /** Path of the cover image relative to the content server's root. */
    val cover: String,
    val chapters: List<ShowcaseChapter>,
)

@Serializable
internal data class ShowcaseChapter(
    val number: Int,
    val name: String,
    /** ISO date the chapter was published. */
    val date: String,
    /** Paths relative to the content server's root: pages in reading order, or the single EPUB or video. */
    val files: List<String>,
)
