package mihon.entry.interactions.runtime

import android.app.Application
import coil3.ComponentRegistry
import mihon.entry.interactions.download.EntryDownloadNotificationActions
import mihon.entry.interactions.library.membership.host.EntryLibraryCustomCoverHost
import mihon.entry.interactions.library.membership.host.EntryLibraryMembershipHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveChildGroupFilterStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCoverHashStateHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveCustomCoverHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveSourceVisibilityHost
import mihon.entry.interactions.lifecycle.profile.host.EntryProfileMoveTrackingStateHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalCustomCoverHost
import mihon.entry.interactions.lifecycle.removal.host.EntryDestructiveRemovalHost
import mihon.entry.interactions.media.EntryViewerSettingsScreenProjectionResolver
import mihon.entry.interactions.media.session.EntryMediaSessionIncognitoState
import mihon.entry.interactions.merge.host.EntryMergeHost
import mihon.entry.interactions.migration.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.migration.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.migration.host.EntryMigrationExecutionHost
import mihon.entry.interactions.migration.host.EntryMigrationPreparationHost
import mihon.entry.interactions.productionEntryFeatureRuntimeModules
import mihon.entry.interactions.productionEntryTypeRuntimeModules
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeInstallation
import mihon.entry.interactions.runtime.production.EntryFeatureRuntimeInstallationContext
import mihon.entry.interactions.runtime.production.installEntryFeatureRuntimeModules
import mihon.entry.interactions.settings.EntryInteractionPreferences
import mihon.entry.interactions.tracking.host.EntryTrackingHost
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.feature.runtime.FeatureRuntimeComposition
import mihon.feature.runtime.FeatureRuntimeInputs
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.domain.entry.model.Entry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektRegistrar
import uy.kohesive.injekt.api.addSingletonFactory
import uy.kohesive.injekt.api.get

data class EntryInteractionRuntimeDependencies(
    val activityTheme: EntryInteractionActivityTheme,
    val notificationActions: EntryDownloadNotificationActions,
    val pageImageCache: EntryPageImageCache,
    val childGroupFilterDataSource: EntryChildGroupFilterDataSource,
    val mediaSessionIncognitoState: EntryMediaSessionIncognitoState,
    val basePreferenceStore: PreferenceStore,
    val profilePreferenceOwners: ProfilePreferenceOwnerInstaller,
    val viewerSettingsScreenProjectionResolver: EntryViewerSettingsScreenProjectionResolver,
    val sourceRefreshUpdateLibraryTitles: (profileId: Long) -> Boolean,
    val libraryMembershipHost: EntryLibraryMembershipHost,
    val libraryCustomCoverHost: EntryLibraryCustomCoverHost,
    val destructiveRemovalHost: EntryDestructiveRemovalHost,
    val destructiveRemovalCustomCoverHost: EntryDestructiveRemovalCustomCoverHost,
    val profileMoveHost: EntryProfileMoveHost,
    val profileMoveSourceVisibilityHost: EntryProfileMoveSourceVisibilityHost,
    val profileMoveCustomCoverHost: EntryProfileMoveCustomCoverHost,
    val profileMoveTrackingStateHost: EntryProfileMoveTrackingStateHost,
    val profileMoveChildGroupFilterStateHost: EntryProfileMoveChildGroupFilterStateHost,
    val profileMoveCoverHashStateHost: EntryProfileMoveCoverHashStateHost,
    val mergeHost: EntryMergeHost,
    val mergeCoverCleanup: suspend (Entry) -> Unit,
    val migrationPreparationHost: EntryMigrationPreparationHost,
    val migrationExecutionHost: EntryMigrationExecutionHost,
    val migrationConsequenceHost: EntryMigrationConsequenceHost,
    val migrationCustomCoverHost: EntryMigrationCustomCoverHost,
    val trackingHost: EntryTrackingHost,
)

data class EntryInteractionRuntimeInstallation(
    val featureRuntimeInputs: FeatureRuntimeInputs,
    val warmups: List<() -> Unit>,
)

fun InjektRegistrar.addEntryInteractionRuntime(
    app: Application,
    dependencies: EntryInteractionRuntimeDependencies,
): EntryInteractionRuntimeInstallation {
    installEntryInteractionHostServices(dependencies)

    val installedFeatureModules = installEntryFeatureRuntimeModules(
        registrar = this,
        modules = productionEntryFeatureRuntimeModules(),
        context = EntryFeatureRuntimeInstallationContext(app, dependencies),
    )
    val typeRuntimeContributions = productionEntryTypeRuntimeModules(
        dependencies.profilePreferenceOwners,
    ).map { module ->
        module.install(this, app).also { it.validate(module.type) }
    }
    val potentialReaderCapabilitiesBySettingsSurface = typeRuntimeContributions
        .flatMap { it.potentialReaderCapabilitiesBySettingsSurface.entries }
        .groupBy({ it.key }, { it.value })
        .mapValues { (_, capabilitySets) -> capabilitySets.flatten().toSet() }
    val chapterPreparationSettingsProvider = ReaderChapterPreparationSettingsProvider(
        preferences = get(),
        potentialCapabilitiesBySettingsSurface = potentialReaderCapabilitiesBySettingsSurface,
    )
    addSingletonFactory { chapterPreparationSettingsProvider }
    addSingletonFactory {
        ReaderSharedSettingsRegistry(
            providers = listOf(chapterPreparationSettingsProvider) +
                typeRuntimeContributions
                    .flatMap(EntryTypeRuntimeContribution::sharedReaderSettingsProviderFactories)
                    .map { it() },
        )
    }

    addSingletonFactory { EntryFeatureRuntimeInstallation(installedFeatureModules) }

    addSingletonFactory {
        EntryImageComponentInstallers(
            typeRuntimeContributions.flatMap(EntryTypeRuntimeContribution::imageComponentInstallers),
        )
    }
    val interactionInstallation = createEntryInteractionInstallation(
        plugins = typeRuntimeContributions.map(EntryTypeRuntimeContribution::plugin),
        featureContributors = installedFeatureModules.flatMap { it.module.graphContributors },
        executionBindings = installedFeatureModules.flatMap { it.artifacts.executionBindings },
        durableExecutionBindings = installedFeatureModules.flatMap {
            it.artifacts.durableExecutionBindings
        },
    )
    addSingletonFactory<EntryInteractions> { interactionInstallation.interactions }
    addSingletonFactory<EntryInteractionComposition> {
        EntryInteractionComposition(
            interactions = get(),
            featureRuntime = get<FeatureRuntimeComposition>(),
        )
    }
    return EntryInteractionRuntimeInstallation(
        featureRuntimeInputs = interactionInstallation.featureRuntimeInputs,
        warmups = installedFeatureModules.flatMap { it.artifacts.warmups } +
            typeRuntimeContributions.flatMap(EntryTypeRuntimeContribution::warmups),
    )
}

private fun InjektRegistrar.installEntryInteractionHostServices(
    dependencies: EntryInteractionRuntimeDependencies,
) {
    addSingletonFactory<EntryInteractionActivityTheme> { dependencies.activityTheme }
    addSingletonFactory<EntryDownloadNotificationActions> { dependencies.notificationActions }
    addSingletonFactory<EntryPageImageCache> { dependencies.pageImageCache }
    addSingletonFactory<EntryMediaSessionIncognitoState> { dependencies.mediaSessionIncognitoState }
    addSingletonFactory<EntryChildGroupFilterDataSource> { dependencies.childGroupFilterDataSource }
    addSingletonFactory { ReaderBasePreferences(dependencies.basePreferenceStore) }
    val chapterPreparationPreferencesOwner = dependencies.profilePreferenceOwners.register(
        ProfilePreferenceOwnerId("entry-interactions.reader.chapter-preparation"),
        factory = ::ReaderChapterPreparationPreferences,
    )
    addSingletonFactory { chapterPreparationPreferencesOwner.create() }
    val entryInteractionPreferencesOwner = dependencies.profilePreferenceOwners.register(
        ProfilePreferenceOwnerId("entry-interactions.preview"),
        factory = ::EntryInteractionPreferences,
    )
    addSingletonFactory { entryInteractionPreferencesOwner.create() }
}

fun ComponentRegistry.Builder.addEntryInteractionImageComponents(): ComponentRegistry.Builder {
    Injekt.get<EntryImageComponentInstallers>().values.forEach { it.install(this) }
    return this
}
