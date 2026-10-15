package mihon.entry.interactions.media

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

internal class DefaultEntryMediaCacheFeature(
    applicableTypes: Set<EntryType>,
    interaction: EntryMediaCacheInteraction,
    preferenceStore: PreferenceStore,
) : EntryMediaCacheFeature {
    private val artifacts: List<EntryMediaCacheArtifact>
    private val artifactsById: Map<EntryMediaCacheId, EntryMediaCacheArtifact>
    private val preferencesById: Map<EntryMediaCacheId, Preference<Boolean>>

    init {
        val providers = applicableTypes.sortedBy { it.ordinal }.map { type ->
            checkNotNull(interaction.provider(type)) {
                "Media-cache type $type has no operational provider"
            }
        }
        providers.forEach { provider ->
            check(provider.artifacts.isNotEmpty()) {
                "Media-cache provider ${provider.type} must contribute at least one artifact"
            }
        }
        artifacts = providers.flatMap(EntryMediaCacheProvider::artifacts)
        artifactsById = artifacts.associateBy(EntryMediaCacheArtifact::id)
        check(artifactsById.size == artifacts.size) {
            "Duplicate media-cache artifact ids: ${artifacts.groupingBy { it.id }.eachCount().duplicates()}"
        }
        val preferencesByKey = artifacts.groupBy { it.autoClearPreference.key }
        check(preferencesByKey.values.all { it.size == 1 }) {
            "Duplicate media-cache preference keys: ${preferencesByKey.filterValues { it.size > 1 }.keys}"
        }

        val existingKeys = preferenceStore.getAll().keys
        preferencesById = artifacts.associate { artifact ->
            val identity = artifact.autoClearPreference
            val preference = preferenceStore.getBoolean(identity.key, false)
            val seedKey = identity.seedFromKeyWhenAbsent
            if (identity.key !in existingKeys && seedKey != null) {
                preference.set(preferenceStore.getBoolean(seedKey, false).get())
            }
            artifact.id to preference
        }
    }

    override fun settings(): List<EntryMediaCacheSetting> {
        return artifacts.map { artifact ->
            EntryMediaCacheSetting(
                id = artifact.id,
                clearLabel = artifact.clearLabel,
                autoClearLabel = artifact.autoClearLabel,
                autoClearOnLaunch = preferencesById.getValue(artifact.id),
            )
        }
    }

    override fun readableSize(id: EntryMediaCacheId): String? = artifactsById[id]?.readableSize

    override fun clear(id: EntryMediaCacheId): EntryMediaCacheClearResult {
        val artifact = artifactsById[id] ?: return EntryMediaCacheClearResult.Inapplicable(id)
        return try {
            EntryMediaCacheClearResult.Cleared(
                id = id,
                deletedFiles = artifact.clear(),
                readableSize = artifact.readableSize,
            )
        } catch (error: Throwable) {
            EntryMediaCacheClearResult.Failed(id, error)
        }
    }

    override fun clearEnabledOnLaunch(): List<EntryMediaCacheClearResult> {
        return artifacts.mapNotNull { artifact ->
            if (preferencesById.getValue(artifact.id).get()) clear(artifact.id) else null
        }
    }
}

private fun Map<EntryMediaCacheId, Int>.duplicates(): Set<EntryMediaCacheId> =
    filterValues { it > 1 }.keys
