package mihon.entry.interactions.anime.download.model

import tachiyomi.core.common.storage.ListedFile

/** A downloaded episode's folder as one listing of it found it; [manifest] is null for a download from before them. */
internal class EpisodePackage(
    val manifest: AnimeDownloadManifest?,
    /** The folder's files by name, matched regardless of case. */
    val files: Map<String, ListedFile>,
)
