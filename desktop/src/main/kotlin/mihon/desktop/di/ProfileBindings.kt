package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.profile.DesktopProfileStore
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.ActiveProfileProvider

@BindingContainer
interface ProfileBindings {
    @Binds
    val DesktopProfileStore.profileStore: ProfileStore

    @Binds
    val DesktopProfileStore.activeProfileProvider: ActiveProfileProvider
}
