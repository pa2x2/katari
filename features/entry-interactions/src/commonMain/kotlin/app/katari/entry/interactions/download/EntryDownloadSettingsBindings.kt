package app.katari.entry.interactions.download

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/**
 * The download settings the entry types offer, kept apart from [EntryDownloadBindings] so that a platform which doesn't
 * host downloads still answers which of them its entry types offer.
 */
@ContributesTo(AppScope::class)
@BindingContainer
object EntryDownloadSettingsBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun settingsFeature(availableSettings: Set<EntryDownloadSetting>): EntryDownloadSettingsFeature =
        DefaultEntryDownloadSettingsFeature(availableSettings)
}
