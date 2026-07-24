package mihon.entry.interactions

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.feature.graph.CapabilityId

data class EntryMediaCacheAutoClearPreference(
    val key: String,
    /** One-time persistence compatibility seed; it is not evidence that this cache is supported. */
    val seedFromKeyWhenAbsent: String? = null,
) {
    init {
        require(key.isNotBlank()) { "Media cache auto-clear preference key cannot be blank" }
        require(seedFromKeyWhenAbsent != key) { "Media cache preference cannot seed itself" }
    }
}

interface EntryMediaCacheArtifact {
    val id: EntryMediaCacheId
    val clearLabel: StringResource
    val autoClearLabel: StringResource
    val autoClearPreference: EntryMediaCacheAutoClearPreference
    val readableSize: String
    fun clear(): Int
}

interface EntryMediaCacheProvider : EntryInteractionProvider {
    /** A supporting provider must expose genuine cache artifacts; absence is represented by no provider binding. */
    val artifacts: List<EntryMediaCacheArtifact>
}

val EntryMediaCacheCapability = entryInteractionCapability<EntryMediaCacheProvider>(
    id = CapabilityId("entry.media-cache"),
)

interface EntryMediaCacheInteraction {
    fun provider(type: EntryType): EntryMediaCacheProvider?
}

internal class ProviderBackedEntryMediaCacheInteraction(
    private val providers: Map<EntryType, EntryMediaCacheProvider>,
) : EntryMediaCacheInteraction {
    override fun provider(type: EntryType): EntryMediaCacheProvider? = providers[type]
}
