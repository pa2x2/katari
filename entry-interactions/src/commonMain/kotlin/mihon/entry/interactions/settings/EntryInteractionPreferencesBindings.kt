package mihon.entry.interactions.settings

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

@ContributesTo(AppScope::class)
@BindingContainer
object EntryInteractionPreferencesBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<EntryInteractionPreferences> =
        owners.register(
            ProfilePreferenceOwnerId("entry-interactions.preview"),
            factory = ::EntryInteractionPreferences,
        )

    @Provides
    @IntoSet
    fun preferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<EntryInteractionPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun preferences(owner: InstalledProfilePreferenceOwner<EntryInteractionPreferences>): EntryInteractionPreferences =
        owner.create()
}
