package app.katari.desktop.di

import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.storage.DesktopDirectories
import app.katari.entry.interactions.source.EntrySourceSettingsFeature
import app.katari.extension.runtime.jvm.application.ExtensionApplication
import app.katari.extension.runtime.jvm.application.MainLooperThread
import app.katari.extension.runtime.jvm.preferences.DesktopSourcePreferenceFileStore
import app.katari.extension.runtime.jvm.preferences.SharedPreferencesDirectory
import app.katari.profiles.core.ProfileDatabase
import app.katari.profiles.core.ProfileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFileStore
import app.katari.profiles.sourcepreferences.SourcePreferenceFiles
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferenceHosts
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.source.identity.SourceIdentityRepository

/**
 * Sources' settings, each profile's own, in the configuration folder (`SourcePreferenceFiles` explains where), and the
 * settings screen's host for them.
 */
@BindingContainer
interface SourcePreferenceBindings {
    @Binds
    val DesktopSourcePreferenceFileStore.sourcePreferenceFileStore: SourcePreferenceFileStore

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun desktopSourcePreferenceFileStore(directories: DesktopDirectories): DesktopSourcePreferenceFileStore =
            DesktopSourcePreferenceFileStore(
                SharedPreferencesDirectory(directories.configuration.resolve("source-settings"), MainLooperThread),
            )

        @Provides
        @SingleIn(AppScope::class)
        fun sourcePreferenceFiles(
            fileStore: SourcePreferenceFileStore,
            profileStore: ProfileStore,
            profileDatabase: ProfileDatabase,
            identities: SourceIdentityRepository,
            @AppCoroutineScope appScope: CoroutineScope,
        ): SourcePreferenceFiles = SourcePreferenceFiles(fileStore, profileStore, profileDatabase, identities, appScope)

        // Desktop has no activities; sources build their preferences with the application they were handed.
        @Provides
        fun sourcePreferenceHosts(
            settingsFeature: EntrySourceSettingsFeature,
            application: ExtensionApplication,
        ): SourcePreferenceHosts = SourcePreferenceHosts(settingsFeature) { application }
    }
}
