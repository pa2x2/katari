package mihon.entry.interactions

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.viewer.settings.ViewerSettingDefinition
import mihon.entry.viewer.settings.ViewerSettingId
import mihon.entry.viewer.settings.ViewerSettingOverride
import mihon.entry.viewer.settings.ViewerSettingOverrideRepository
import mihon.entry.viewer.settings.ViewerSettingScope
import mihon.entry.viewer.settings.ViewerSettingsProvider
import mihon.feature.graph.CapabilityExpression
import mihon.feature.graph.ContributionOwner
import mihon.feature.graph.FeatureArtifactId
import mihon.feature.graph.FeatureBehaviorContract
import mihon.feature.graph.FeatureBehaviorProjection
import mihon.feature.graph.FeatureContribution
import mihon.feature.graph.FeatureGraphContributionSink
import mihon.feature.graph.FeatureGraphContributor
import mihon.feature.graph.FeatureGraphEvaluation
import mihon.feature.graph.FeatureId
import mihon.feature.graph.FeatureIntegration
import mihon.feature.graph.FeatureIntegrationId
import mihon.feature.graph.allOf
import tachiyomi.domain.entry.model.Entry

internal fun interface EntryLegacyMangaViewerFlagsReset {
    suspend fun reset(): Boolean
}

internal fun interface EntryViewerFlagsMigrationStore {
    suspend fun update(entryId: Long, profileId: Long, viewerFlags: Long): Boolean
}

internal class DefaultEntryViewerSettingsFeature(
    evaluation: FeatureGraphEvaluation,
    interaction: EntryViewerSettingsInteraction,
    projectionResolver: EntryViewerSettingsScreenProjectionResolver,
    private val overrideRepository: ViewerSettingOverrideRepository,
    private val legacyMangaViewerFlagsReset: EntryLegacyMangaViewerFlagsReset,
    private val migrationStore: EntryViewerFlagsMigrationStore,
) : EntryViewerSettingsFeature {
    private val applicableTypes = EntryViewerSettingsBehavior.entries
        .map { behavior ->
            evaluation.applicableProviderTypes<EntryViewerSettingsProvider>(
                feature = ENTRY_VIEWER_SETTINGS_FEATURE_ID,
                integration = ENTRY_VIEWER_SETTINGS_PROVIDER_INTEGRATION_ID,
                behaviorProjection = behavior.id,
            )
        }
        .also { selected ->
            check(selected.distinct().size <= 1) {
                "Viewer Settings behaviors selected different provider sets: $selected"
            }
        }
        .firstOrNull()
        .orEmpty()
    private val migrationTypes = evaluation.applicableProviderTypes<EntryViewerSettingsProvider>(
        feature = ENTRY_VIEWER_SETTINGS_FEATURE_ID,
        integration = ENTRY_VIEWER_SETTINGS_MIGRATION_INTEGRATION_ID,
        behaviorProjection = EntryViewerSettingsMigrationBehavior.id,
    )

    private val providersByType = applicableTypes.associateWith { type ->
        requireNotNull(interaction.provider(type)) {
            "Viewer Settings graph selected $type without an operational provider"
        }
    }
    private val surfacesById = providersByType.values
        .flatMap { provider -> provider.surfaces.map { surface -> provider.type to surface } }
        .also { surfaces ->
            val duplicateIds = surfaces.groupingBy { it.second.id }.eachCount().filterValues { it > 1 }.keys
            check(duplicateIds.isEmpty()) { "Duplicate Viewer Settings surface IDs: $duplicateIds" }
        }
        .associateBy { it.second.id }
    private val projectionsById = projectionResolver.resolve(surfacesById.keys)
        .also { values ->
            val duplicateIds = values.groupingBy(EntryViewerSettingsScreenProjection::surfaceId)
                .eachCount()
                .filterValues { it > 1 }
                .keys
            check(duplicateIds.isEmpty()) { "Duplicate Viewer Settings screen projections: $duplicateIds" }
        }
        .associateBy(EntryViewerSettingsScreenProjection::surfaceId)

    override val destinations: List<EntryViewerSettingsDestination>

    init {
        val missingProjections = surfacesById.keys - projectionsById.keys
        check(missingProjections.isEmpty()) {
            "Viewer Settings providers are missing app screen projections: ${missingProjections.sorted()}"
        }
        val orphanProjections = projectionsById.keys - surfacesById.keys
        check(orphanProjections.isEmpty()) {
            "Viewer Settings screen projections have no provider surface: ${orphanProjections.sorted()}"
        }

        destinations = surfacesById.values
            .map { (type, surface) -> surface.toDestination(type, projectionsById.getValue(surface.id)) }
            .sortedWith(compareBy({ it.category.name }, { it.type.name }, { it.displayName }, { it.surfaceId }))
    }

    override fun isApplicable(type: EntryType): Boolean = type in applicableTypes

    override suspend fun snapshot(entry: Entry): EntryViewerSettingsSnapshotResult {
        val definitions = overrideDefinitions(entry.type)
            ?: return EntryViewerSettingsSnapshotResult.Inapplicable(entry.type)
        val overrides = overrideRepository.getByEntryId(entry.id)
            .filter { override -> definitions[override.settingId]?.accepts(override.encodedValue) == true }
        return EntryViewerSettingsSnapshotResult.Available(overrides)
    }

    override suspend fun restore(
        entry: Entry,
        overrides: List<ViewerSettingOverride>,
    ): EntryViewerSettingsRestoreResult {
        val definitions = overrideDefinitions(entry.type)
            ?: return EntryViewerSettingsRestoreResult.Inapplicable(entry.type)
        val accepted = linkedMapOf<ViewerSettingId, ViewerSettingOverride>()
        val rejected = linkedSetOf<ViewerSettingId>()
        overrides.forEach { override ->
            val definition = definitions[override.settingId]
            if (definition?.accepts(override.encodedValue) == true) {
                accepted[override.settingId] = override.copy(entryId = entry.id)
            } else {
                rejected += override.settingId
            }
        }
        accepted.values.forEach { overrideRepository.upsert(it) }
        return EntryViewerSettingsRestoreResult.Restored(
            restoredCount = accepted.size,
            rejectedSettingIds = rejected,
        )
    }

    override suspend fun copy(source: Entry, target: Entry): EntryViewerSettingsCopyResult {
        if (source.type !in migrationTypes || target.type !in migrationTypes) {
            return EntryViewerSettingsCopyResult.Inapplicable(source.type, target.type)
        }
        val sourceDefinitions = overrideDefinitions(source.type)
            ?: return EntryViewerSettingsCopyResult.Inapplicable(source.type, target.type)
        val targetDefinitions = overrideDefinitions(target.type)
            ?: return EntryViewerSettingsCopyResult.Inapplicable(source.type, target.type)
        val sharedDefinitions = sourceDefinitions.keys intersect targetDefinitions.keys
        val accepted = overrideRepository.getByEntryId(source.id)
            .filter { override ->
                override.settingId in sharedDefinitions &&
                    sourceDefinitions.getValue(override.settingId).accepts(override.encodedValue) &&
                    targetDefinitions.getValue(override.settingId).accepts(override.encodedValue)
            }
            .associateBy { it.settingId }
            .values
        accepted.forEach { override -> overrideRepository.upsert(override.copy(entryId = target.id)) }
        return EntryViewerSettingsCopyResult.Copied(accepted.size)
    }

    override suspend fun prepareMigration(
        source: Entry,
        target: Entry,
    ): EntryViewerSettingsMigrationPreparation {
        if (source.type != target.type) {
            return EntryViewerSettingsMigrationPreparation.TypeMismatch(source.type, target.type)
        }
        val migrationInapplicableTypes = setOf(source.type, target.type) - migrationTypes
        if (migrationInapplicableTypes.isNotEmpty()) {
            return EntryViewerSettingsMigrationPreparation.Inapplicable(migrationInapplicableTypes)
        }
        val sourceDefinitions = overrideDefinitions(source.type)
        val targetDefinitions = overrideDefinitions(target.type)
        if (sourceDefinitions == null || targetDefinitions == null) {
            return EntryViewerSettingsMigrationPreparation.Inapplicable(setOf(source.type, target.type))
        }
        val sharedDefinitions = sourceDefinitions.keys intersect targetDefinitions.keys
        val overrides = overrideRepository.getByEntryId(source.id)
            .filter { override ->
                override.settingId in sharedDefinitions &&
                    sourceDefinitions.getValue(override.settingId).accepts(override.encodedValue) &&
                    targetDefinitions.getValue(override.settingId).accepts(override.encodedValue)
            }
            .associateBy { it.settingId }
            .values
            .map { override ->
                EntryViewerSettingMigrationValue(
                    providerId = override.settingId.providerId,
                    settingKey = override.settingId.key,
                    encodedValue = override.encodedValue,
                    updatedAt = override.updatedAt,
                )
            }
        val normalizedFlags = providersByType.getValue(source.type).normalizeLegacyViewerFlags(source.viewerFlags)
        return EntryViewerSettingsMigrationPreparation.Prepared(
            EntryViewerSettingsMigrationPayload(target, normalizedFlags, overrides),
        )
    }

    override suspend fun applyMigration(
        payload: EntryViewerSettingsMigrationPayload,
    ): EntryViewerSettingsRestoreResult {
        check(
            migrationStore.update(payload.target.id, payload.target.profileId, payload.normalizedViewerFlags),
        ) { "Viewer flags were not persisted for Migration target ${payload.target.id}" }
        return restore(
            payload.target,
            payload.overrides.map { value ->
                ViewerSettingOverride(
                    entryId = payload.target.id,
                    settingId = ViewerSettingId(value.providerId, value.settingKey),
                    encodedValue = value.encodedValue,
                    updatedAt = value.updatedAt,
                )
            },
        )
    }

    override suspend fun resetProfileOverrides(profileId: Long): EntryViewerSettingsResetResult {
        surfacesById.keys.forEach { surfaceId ->
            overrideRepository.deleteByProviderForProfile(surfaceId, profileId)
        }
        return if (legacyMangaViewerFlagsReset.reset()) {
            EntryViewerSettingsResetResult.Reset
        } else {
            EntryViewerSettingsResetResult.LegacyViewerFlagsFailed
        }
    }

    private fun overrideDefinitions(type: EntryType): Map<ViewerSettingId, ViewerSettingDefinition<*>>? {
        val provider = providersByType[type] ?: return null
        return provider.surfaces
            .flatMap(ViewerSettingsProvider::settings)
            .filter { it.scope == ViewerSettingScope.PROFILE_WITH_ENTRY_OVERRIDE }
            .associateBy(ViewerSettingDefinition<*>::id)
    }
}

private fun ViewerSettingDefinition<*>.accepts(encodedValue: String): Boolean {
    val decoded = decode(encodedValue) ?: return false
    return validateDecoded(decoded)
}

@Suppress("UNCHECKED_CAST")
private fun ViewerSettingDefinition<*>.decode(encodedValue: String): Any? =
    (codec as mihon.entry.viewer.settings.ViewerSettingCodec<Any>).decode(encodedValue)

@Suppress("UNCHECKED_CAST")
private fun ViewerSettingDefinition<*>.validateDecoded(value: Any): Boolean =
    (validate as (Any) -> Boolean)(value)

private fun ViewerSettingsProvider.toDestination(
    type: EntryType,
    projection: EntryViewerSettingsScreenProjection,
) = EntryViewerSettingsDestination(
    type = type,
    surfaceId = id,
    category = category,
    displayName = displayName,
    description = description,
    origin = origin,
    projection = projection,
)
