package mihon.entry.interactions

import android.app.Application
import coil3.ComponentRegistry
import mihon.entry.interactions.EntryLibraryCustomCoverHost
import mihon.entry.interactions.EntryLibraryMembershipHost
import mihon.entry.interactions.host.EntryMergeHost
import mihon.entry.interactions.host.EntryMigrationConsequenceHost
import mihon.entry.interactions.host.EntryMigrationCustomCoverHost
import mihon.entry.interactions.host.EntryMigrationExecutionHost
import mihon.entry.interactions.host.EntryMigrationPreparationHost
import mihon.entry.interactions.host.tracking.EntryTrackingHost
import mihon.entry.interactions.reader.settings.ReaderBasePreferences
import mihon.entry.interactions.settings.EntryInteractionPreferences
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

fun interface EntryInteractionRuntimeWarmup {
    fun warmup()
}

fun InjektRegistrar.addEntryInteractionRuntime(
    app: Application,
    dependencies: EntryInteractionRuntimeDependencies,
) {
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

    addSingletonFactory { EntryFeatureRuntimeInstallation(installedFeatureModules) }

    addSingletonFactory {
        EntryImageComponentInstallers(
            typeRuntimeContributions.flatMap(EntryTypeRuntimeContribution::imageComponentInstallers),
        )
    }
    addSingletonFactory<EntryInteractionComposition> {
        createEntryInteractionComposition(
            plugins = typeRuntimeContributions.map(EntryTypeRuntimeContribution::plugin),
            featureContributors = installedFeatureModules.flatMap { it.module.graphContributors },
            executionBindings = installedFeatureModules.flatMap { it.artifacts.executionBindings },
            durableExecutionBindings = installedFeatureModules.flatMap {
                it.artifacts.durableExecutionBindings
            },
        )
    }
    addSingletonFactory<EntryInteractionRuntimeWarmup> {
        EntryInteractionRuntimeWarmup {
            installedFeatureModules.flatMap { it.artifacts.warmups }.forEach { it() }
            typeRuntimeContributions.flatMap(EntryTypeRuntimeContribution::warmups).forEach { it() }
        }
    }
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
