package mihon.domain.extension.model

data class ExtensionStore(
    val indexUrl: String,
    val name: String,
    val badgeLabel: String,
    val signingKey: String,
    val contact: Contact,
    val isLegacy: Boolean,
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
