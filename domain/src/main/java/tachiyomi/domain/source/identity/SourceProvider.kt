package tachiyomi.domain.source.identity

/** The extension that provides a source, as far as deciding between two that claim the same identity goes. */
data class SourceProvider(
    val kind: SourceProviderKind,
    /** Null for a source built into the app. */
    val packageName: String?,
)

/**
 * The kind of extension that provided a source, recorded so a later version can give one kind its own
 * [SourceIdScheme] by rewriting the registry without changing any local key.
 */
@JvmInline
value class SourceProviderKind(val key: String) {
    companion object {
        /** Built against the Tachiyomi extensions-lib (1.4 and 1.6). */
        val TACHIYOMI_EXTENSION = SourceProviderKind("tachiyomi-extension")

        /** Built against Katari's Entry SDK (2.x). */
        val KATARI_EXTENSION = SourceProviderKind("katari-extension")

        /** Built against Aniyomi's extensions-lib (14 to 17) and run through the Aniyomi runtime. */
        val ANIYOMI_EXTENSION = SourceProviderKind("aniyomi-extension")

        /** An LNReader plugin, run through the LNReader adapter. */
        val LNREADER_PLUGIN = SourceProviderKind("lnreader-plugin")
        val BUILT_IN = SourceProviderKind("built-in")
    }
}
