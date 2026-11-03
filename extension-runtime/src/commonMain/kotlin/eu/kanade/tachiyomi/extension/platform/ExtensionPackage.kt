package eu.kanade.tachiyomi.extension.platform

/** An apk as the platform reads it: one installed for every app, a private copy, or a download. */
interface ExtensionPackage {
    val packageName: String
    val versionName: String?
    val versionCode: Long

    /** The `uses-feature` names it declares, which tell the extension kinds it's one of theirs. */
    val features: Set<String>

    /** `null` for a package without application meta-data, which no extension kind can describe. */
    val metaData: ExtensionMetaData?

    /**
     * The SHA-256 of each signer's certificate, `null` when it isn't signed. With several signers, those of its
     * contents; with one, its certificate lineage oldest first, so the last is the one it's signed with now.
     */
    val signatures: List<String>?

    /** Whether it's installed for every app rather than as the app's private copy. */
    val isShared: Boolean

    /** Its application label, read only when it names itself nowhere else, since reading it can take resources. */
    fun label(): String
}
