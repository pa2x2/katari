package mihon.entry.interactions

import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureGraphEvaluation
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

internal class DefaultEntryMediaCacheFeature(
    evaluation: FeatureGraphEvaluation,
    interaction: EntryMediaCacheInteraction,
    preferenceStore: PreferenceStore,
) : EntryMediaCacheFeature {
    private val artifacts: List<EntryMediaCacheArtifact>
    private val artifactsById: Map<EntryMediaCacheId, EntryMediaCacheArtifact>
    private val preferencesById: Map<EntryMediaCacheId, Preference<Boolean>>

    init {
        val typesByBehavior = listOf(
            ENTRY_MEDIA_CACHE_DISCOVERY_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_SETTINGS_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_MANUAL_CLEAR_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_LAUNCH_CLEAR_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_PREFERENCES_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_INVALIDATION_BEHAVIOR_ID,
            ENTRY_MEDIA_CACHE_ERRORS_BEHAVIOR_ID,
        ).associateWith { behavior ->
            evaluation.applicableProviderTypes<EntryMediaCacheProvider>(
                feature = ENTRY_MEDIA_CACHE_FEATURE_ID,
                integration = ENTRY_MEDIA_CACHE_INTEGRATION_ID,
                behaviorProjection = behavior,
            )
        }
        check(typesByBehavior.values.toSet().size == 1) {
            "Media-cache behaviors selected different provider sets: ${typesByBehavior.describe()}"
        }

        val applicableTypes = typesByBehavior.getValue(ENTRY_MEDIA_CACHE_DISCOVERY_BEHAVIOR_ID)
        val providers = applicableTypes.sortedBy { it.ordinal }.map { type ->
            checkNotNull(interaction.provider(type)) {
                "Media-cache graph selected $type without an operational provider"
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
                readableSize = artifact.readableSize,
                autoClearOnLaunch = preferencesById.getValue(artifact.id),
            )
        }
    }

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

private fun Map<FeatureArtifactId, Set<*>>.describe(): String =
    entries.joinToString { (behavior, types) -> "${behavior.value}=$types" }

private fun Map<EntryMediaCacheId, Int>.duplicates(): Set<EntryMediaCacheId> =
    filterValues { it > 1 }.keys
