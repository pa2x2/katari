package app.katari.android.di

import app.katari.profiles.core.AndroidLegacyProfilePreferenceKeys
import app.katari.profiles.core.LegacyProfilePreferenceKeys
import app.katari.profiles.core.ProfilePreferenceOwnership
import app.katari.profiles.ui.AndroidProfileUnlock
import app.katari.profiles.ui.ProfileUnlock
import app.katari.profiles.ui.switcher.AndroidProfileSwitchEffects
import app.katari.profiles.ui.switcher.ProfileSwitchEffects
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

@BindingContainer
interface ProfileBindings {
    @Binds
    val AndroidLegacyProfilePreferenceKeys.legacyProfilePreferenceKeys: LegacyProfilePreferenceKeys

    @Binds
    val AndroidProfileUnlock.profileUnlock: ProfileUnlock

    @Binds
    val AndroidProfileSwitchEffects.profileSwitchEffects: ProfileSwitchEffects

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        fun profilePreferenceOwnership(owners: ProfilePreferenceOwnerRegistry): ProfilePreferenceOwnership =
            ProfilePreferenceOwnership(owners)
    }
}
