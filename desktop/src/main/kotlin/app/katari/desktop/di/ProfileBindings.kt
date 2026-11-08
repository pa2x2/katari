package app.katari.desktop.di

import app.katari.core.common.platform.DesktopOperatingSystem
import app.katari.core.metro.AppCoroutineScope
import app.katari.desktop.presence.FreedesktopScreenLock
import app.katari.desktop.presence.ScreenLock
import app.katari.desktop.profile.DesktopProfileStore
import app.katari.desktop.profile.DesktopProfileSwitchEffects
import app.katari.desktop.profile.DesktopProfileUnlock
import app.katari.desktop.profile.NoLegacyProfilePreferenceKeys
import app.katari.profiles.core.LegacyProfilePreferenceKeys
import app.katari.profiles.core.ProfileStore
import app.katari.profiles.ui.ProfileUnlock
import app.katari.profiles.ui.switcher.ProfileSwitchEffects
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
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
