package mihon.desktop.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import mihon.core.common.platform.DesktopOperatingSystem
import mihon.core.metro.AppCoroutineScope
import mihon.desktop.presence.FreedesktopScreenLock
import mihon.desktop.presence.ScreenLock
import mihon.desktop.profile.DesktopProfileStore
import mihon.desktop.profile.DesktopProfileSwitchEffects
import mihon.desktop.profile.DesktopProfileUnlock
import mihon.desktop.profile.NoLegacyProfilePreferenceKeys
import mihon.feature.profiles.core.LegacyProfilePreferenceKeys
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
    val DesktopProfileUnlock.profileUnlock: ProfileUnlock

    @Binds
    val DesktopProfileSwitchEffects.profileSwitchEffects: ProfileSwitchEffects

    companion object {
        // The screen lock starts a profile's "Lock when idle" clock, as the app going to the background does on Android
        @Provides
        @SingleIn(AppScope::class)
        fun screenLock(
            operatingSystem: DesktopOperatingSystem,
            @AppCoroutineScope scope: CoroutineScope,
        ): ScreenLock = when (operatingSystem) {
            DesktopOperatingSystem.LINUX -> FreedesktopScreenLock(scope)
        }
    }
}
