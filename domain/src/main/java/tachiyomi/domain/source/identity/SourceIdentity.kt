package tachiyomi.domain.source.identity

/**
 * Who a source is across ecosystems. An extension's source ID is only unique inside the ecosystem that minted it, so
 * the same [externalId] can name unrelated sources in two schemes. Katari itself keys a source by the local key
 * [SourceIdentityRepository] maps this to; the extension keeps seeing [externalId].
 */
data class SourceIdentity(
    val scheme: SourceIdScheme,
    val externalId: Long,
) {
    companion object {
        val LOCAL = SourceIdentity(SourceIdScheme.LOCAL, LOCAL_SOURCE_ID)

        /** What a key stored before identities were recorded stood for, as in a backup without them. */
        fun ofLegacyKey(key: Long): SourceIdentity {
            return if (key == LOCAL_SOURCE_ID) LOCAL else SourceIdentity(SourceIdScheme.TACHIYOMI, key)
        }
    }
}

/**
 * The ecosystem whose rules minted a source's ID, declared by the loader that loads the extension and never by the
 * extension. Kept as an open key rather than an enum so a backup or database written by a later version, with a scheme
 * this one doesn't know, still restores its sources as missing ones instead of failing.
 */
@JvmInline
value class SourceIdScheme(val key: String) {

    /** Holds for the schemes Katari stored before identities were recorded, so their data didn't move. */
    val externalIdIsLocalKey: Boolean
        get() = this == TACHIYOMI || this == LOCAL

    companion object {
        /**
         * Tachiyomi, Mihon and Keiyoushi extensions, which by convention mean the same source by the same ID, and Katari
         * Entry SDK extensions, which share the scheme so a port can take over a Keiyoushi source's ID without a migration.
         */
        val TACHIYOMI = SourceIdScheme("tachiyomi")
        val LOCAL = SourceIdScheme("local")

        /** Aniyomi extensions, whose IDs can repeat a Tachiyomi source's, so their sources get keys of their own. */
        val ANIYOMI = SourceIdScheme("aniyomi")

        /** LNReader plugins, whose text IDs become external IDs through [lnReaderExternalId]. */
        val LNREADER = SourceIdScheme("lnreader")
    }
}

const val LOCAL_SOURCE_ID = 0L
