package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import eu.kanade.domain.ui.UiPreferences
import mihon.desktop.storage.DesktopDirectories
import tachiyomi.core.common.preference.DesktopPreferenceStore
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.upcoming.service.UpcomingPreferences
import tachiyomi.domain.updates.service.UpdatesPreferences

/**
 * Desktop has no profiles yet: preferences Android keeps per profile, such as [UiPreferences], live in this one store.
 */
@BindingContainer
object PreferenceBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun preferenceStore(directories: DesktopDirectories): PreferenceStore =
        DesktopPreferenceStore(directories.configuration.resolve("preferences.json"))

    @Provides
    @SingleIn(AppScope::class)
    fun uiPreferences(preferenceStore: PreferenceStore): UiPreferences = UiPreferences(preferenceStore)

    @Provides
    @SingleIn(AppScope::class)
    fun libraryPreferences(preferenceStore: PreferenceStore): LibraryPreferences = LibraryPreferences(preferenceStore)

    @Provides
    @SingleIn(AppScope::class)
    fun downloadPreferences(preferenceStore: PreferenceStore): DownloadPreferences =
        DownloadPreferences(preferenceStore)

    @Provides
    @SingleIn(AppScope::class)
    fun updatesPreferences(preferenceStore: PreferenceStore): UpdatesPreferences = UpdatesPreferences(preferenceStore)

    @Provides
    @SingleIn(AppScope::class)
    fun upcomingPreferences(preferenceStore: PreferenceStore): UpcomingPreferences =
        UpcomingPreferences(preferenceStore)
}
