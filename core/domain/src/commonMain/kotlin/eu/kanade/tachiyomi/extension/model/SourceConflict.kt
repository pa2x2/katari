package eu.kanade.tachiyomi.extension.model

/** A source an extension has but doesn't provide, because another one claims the same identity or local key. */
data class SourceConflict(
    val sourceName: String,
    val sourceLang: String,
    /**
     * The name of the extension that provides the source instead. Null when the key belongs to a source built into the
     * app, or to one whose extension isn't loaded.
     */
    val providedBy: String?,
    /** The source's local key when this extension could provide it instead, null when the key is another source's. */
    val sourceKey: Long?,
)
