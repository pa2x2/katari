package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.entry.interactions.settings.AnimePlayerDevicePreferences
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.PreferenceStore

/** The unqualified preference store, and the preference classes Android's type modules provide there. */
@BindingContainer
object PreferenceBindings {
    // On Android, too, the unqualified store is the base store's file.
    @Provides
    fun preferenceStore(profileStore: ProfileStore): PreferenceStore = profileStore.basePreferenceStore()

    @Provides
    @SingleIn(AppScope::class)
    fun animePlayerDevicePreferences(profileStore: ProfileStore): AnimePlayerDevicePreferences =
        AnimePlayerDevicePreferences(profileStore.basePreferenceStore())
}
