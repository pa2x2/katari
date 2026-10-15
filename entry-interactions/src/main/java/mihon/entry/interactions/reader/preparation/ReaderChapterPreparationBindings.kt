package mihon.entry.interactions.reader.preparation

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.metro.StartupWarmup
import mihon.entry.interactions.runtime.ReaderSettingsSurfaceCapabilities
import tachiyomi.core.common.preference.InstalledProfilePreferenceOwner
import tachiyomi.core.common.preference.ProfilePreferenceKeyPattern
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller

@ContributesTo(AppScope::class)
@BindingContainer
object ReaderChapterPreparationBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun preferencesOwner(
        owners: ProfilePreferenceOwnerInstaller,
    ): InstalledProfilePreferenceOwner<ReaderChapterPreparationPreferences> = owners.register(
        id = ProfilePreferenceOwnerId("entry-interactions.reader.chapter-preparation"),
        keyPatterns = setOf(
            ProfilePreferenceKeyPattern.Prefix(ReaderChapterPreparationPreferences.SURFACE_KEY_PREFIX),
        ),
        factory = ::ReaderChapterPreparationPreferences,
    )

    @Provides
    @IntoSet
    fun preferencesOwnerRegistration(
        owner: InstalledProfilePreferenceOwner<ReaderChapterPreparationPreferences>,
    ): InstalledProfilePreferenceOwner<*> = owner

    @Provides
    @SingleIn(AppScope::class)
    fun preferences(
        owner: InstalledProfilePreferenceOwner<ReaderChapterPreparationPreferences>,
    ): ReaderChapterPreparationPreferences = owner.create()

    @Provides
    @SingleIn(AppScope::class)
    fun settingsProvider(
        preferences: ReaderChapterPreparationPreferences,
        surfaceCapabilities: Set<ReaderSettingsSurfaceCapabilities>,
    ): ReaderChapterPreparationSettingsProvider = ReaderChapterPreparationSettingsProvider(
        preferences = preferences,
        potentialCapabilitiesBySettingsSurface = surfaceCapabilities
            .groupBy({ it.surfaceId }, { it.capabilities })
            .mapValues { (_, capabilitySets) -> capabilitySets.flatten().toSet() },
    )

    // The provider completes the legacy chapter preparation migration when it is created, which has to happen
    // before a reader reads the per-surface preferences.
    @Provides
    @IntoSet
    fun settingsProviderWarmup(provider: () -> ReaderChapterPreparationSettingsProvider): StartupWarmup =
        StartupWarmup { provider() }
}
