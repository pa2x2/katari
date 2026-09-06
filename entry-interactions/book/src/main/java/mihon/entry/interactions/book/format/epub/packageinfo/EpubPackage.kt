package mihon.entry.interactions.book.format.epub.packageinfo

internal data class EpubPackage(
    val packageResource: String,
    val manifest: Map<String, EpubManifestItem>,
    val documents: List<EpubManifestItem>,
    val readingOrder: List<EpubManifestItem>,
    val navigationResource: EpubManifestItem?,
    val legacyNavigationResource: EpubManifestItem?,
    val uniqueIdentifier: String?,
    val resourceProtectionAlgorithms: Map<String, String>,
    val languages: List<String>,
    val rightToLeft: Boolean,
)
