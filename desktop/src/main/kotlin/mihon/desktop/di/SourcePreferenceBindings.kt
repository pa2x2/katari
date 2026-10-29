package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.source.entry.EntryPreferenceProvider
import kotlinx.coroutines.CoroutineScope
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.storage.DesktopDirectories
import mihon.extension.runtime.jvm.application.MainLooperThread
import mihon.extension.runtime.jvm.preferences.DesktopSourcePreferenceFileStore
import mihon.extension.runtime.jvm.preferences.SharedPreferencesDirectory
import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.sourcepreferences.ProfileSourcePreferenceProvider
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFileStore
import mihon.feature.profiles.sourcepreferences.SourcePreferenceFiles
import tachiyomi.domain.source.identity.SourceIdentityRepository

/** Sources' settings, each profile's own, in the configuration folder; `SourcePreferenceFiles` explains where. */
@BindingContainer
interface SourcePreferenceBindings {
    @Binds
    val DesktopSourcePreferenceFileStore.sourcePreferenceFileStore: SourcePreferenceFileStore

    @Binds
    val ProfileSourcePreferenceProvider.entryPreferenceProvider: EntryPreferenceProvider

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

        @Provides
        @SingleIn(AppScope::class)
        fun profileSourcePreferenceProvider(files: SourcePreferenceFiles): ProfileSourcePreferenceProvider =
            ProfileSourcePreferenceProvider(files)
    }
}
