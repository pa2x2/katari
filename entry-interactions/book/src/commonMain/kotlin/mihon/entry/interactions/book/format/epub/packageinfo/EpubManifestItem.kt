package mihon.entry.interactions.book.format.epub.packageinfo

internal data class EpubManifestItem(
    val id: String,
    val resourceId: String,
    val mediaType: String,
    val properties: Set<String>,
    val isRemote: Boolean = false,
)
