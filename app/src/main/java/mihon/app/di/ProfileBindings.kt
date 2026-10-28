package mihon.app.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.feature.profiles.core.AndroidLegacyProfilePreferenceKeys
import mihon.feature.profiles.core.LegacyProfilePreferenceKeys
import mihon.feature.profiles.core.ProfileExtensions
import mihon.feature.profiles.core.ProfilePreferenceOwnership
import mihon.feature.profiles.sourcepreferences.DefaultProfileExtensions
import mihon.feature.profiles.ui.AndroidProfileUnlock
import mihon.feature.profiles.ui.ProfileUnlock
import mihon.feature.profiles.ui.switcher.AndroidProfileSwitchEffects
import mihon.feature.profiles.ui.switcher.ProfileSwitchEffects
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

@BindingContainer
interface ProfileBindings {
    @Binds
    val AndroidLegacyProfilePreferenceKeys.legacyProfilePreferenceKeys: LegacyProfilePreferenceKeys

    @Binds
    val DefaultProfileExtensions.profileExtensions: ProfileExtensions

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
