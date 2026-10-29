package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.core.common.preference.PreferenceStore

@BindingContainer
object PreferenceBindings {
    // On Android, too, the unqualified store is the base store's file.
    @Provides
    fun preferenceStore(profileStore: ProfileStore): PreferenceStore = profileStore.basePreferenceStore()
}
