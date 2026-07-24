package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import tachiyomi.core.common.preference.Preference

@JvmInline
value class EntryMediaCacheId(val value: String) {
    init {
        require(value.isNotBlank()) { "Media cache id cannot be blank" }
    }
}

data class EntryMediaCacheSetting(
    val id: EntryMediaCacheId,
    val clearLabel: StringResource,
    val autoClearLabel: StringResource,
    val readableSize: String,
    val autoClearOnLaunch: Preference<Boolean>,
)

sealed interface EntryMediaCacheClearResult {
    val id: EntryMediaCacheId

    data class Cleared(
        override val id: EntryMediaCacheId,
        val deletedFiles: Int,
        val readableSize: String,
    ) : EntryMediaCacheClearResult

    data class Failed(
        override val id: EntryMediaCacheId,
        val error: Throwable,
    ) : EntryMediaCacheClearResult

    data class Inapplicable(
        override val id: EntryMediaCacheId,
    ) : EntryMediaCacheClearResult
}

interface EntryMediaCacheFeature {
    /** Current settings projection for every cache contributed by an applicable type provider. */
    fun settings(): List<EntryMediaCacheSetting>

    /** Clears one currently applicable cache without exposing its provider to application code. */
    fun clear(id: EntryMediaCacheId): EntryMediaCacheClearResult

    /** Clears every applicable cache whose shared launch preference is enabled. */
    fun clearEnabledOnLaunch(): List<EntryMediaCacheClearResult>
}
