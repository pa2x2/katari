package mihon.domain.extension.model

data class ExtensionStore(
    val indexUrl: String,
    val name: String,
    val badgeLabel: String,
    /** The key its extensions are signed with; `null` for a store whose extensions nobody signs. */
    val signingKey: String?,
    val contact: Contact,
    val format: ExtensionStoreFormat,
    val extensionListUrl: String? = null,
) {
    data class Contact(
        val website: String,
        val discord: String?,
    )

    companion object {
        // Stores migrated from the old repository preference carry this until a refresh fetches their real key.
        const val NO_SIGNING_KEY = "NO_SIGNING_KEY"
    }
}

/**
 * How a store's index is read, as found when the store was added. Kept as an open key rather than an enum so a backup
 * or database from a later version, with a format this one doesn't know, still restores the store, which then lists
 * nothing until the app can read it.
 */
@JvmInline
value class ExtensionStoreFormat(val key: String) {
    companion object {
        /** A `repo.json` beside an `index.min.json` array (Tachiyomi, Mihon and Aniyomi legacy stores). */
        val LEGACY = ExtensionStoreFormat("legacy")

        /** A v2 index, JSON or protobuf, in Katari's or Aniyomi's layout. */
        val V2 = ExtensionStoreFormat("v2")

        /** An LNReader `plugins.min.json` array, which lists plugin files and signs nothing. */
        val LNREADER = ExtensionStoreFormat("lnreader")
    }
}
