package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import mihon.desktop.profile.DesktopProfileStore
import mihon.desktop.profile.DesktopProfileSwitchEffects
import mihon.desktop.profile.NoLegacyProfilePreferenceKeys
import mihon.desktop.profile.NoProfileUnlock
import mihon.desktop.source.NoProfileExtensions
import mihon.feature.profiles.core.LegacyProfilePreferenceKeys
import mihon.feature.profiles.core.ProfileExtensions
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.ui.ProfileUnlock
import mihon.feature.profiles.ui.switcher.ProfileSwitchEffects
import tachiyomi.data.ActiveProfileProvider

@BindingContainer
interface ProfileBindings {
    @Binds
    val DesktopProfileStore.profileStore: ProfileStore

    @Binds
    val DesktopProfileStore.activeProfileProvider: ActiveProfileProvider

    @Binds
    val NoLegacyProfilePreferenceKeys.legacyProfilePreferenceKeys: LegacyProfilePreferenceKeys

    @Binds
    val NoProfileExtensions.profileExtensions: ProfileExtensions

    @Binds
    val NoProfileUnlock.profileUnlock: ProfileUnlock

    @Binds
    val DesktopProfileSwitchEffects.profileSwitchEffects: ProfileSwitchEffects
}
