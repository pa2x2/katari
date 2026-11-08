package app.katari.ui.di

import app.katari.ui.buildinfo.AppBuild
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdateIntervalAvailability
import eu.kanade.tachiyomi.ui.library.ReleasePeriodFilterAvailability
import tachiyomi.domain.library.service.LibraryUpdateQueue

/**
 * Update intervals and release periods follow what library updates work out, so they are offered where the platform
 * runs library updates, unless the build holds them back.
 */
// TODO: offer them in stable releases when custom intervals are ready for stable
@ContributesTo(AppScope::class)
@BindingContainer
object LibraryUpdateAvailabilityBindings {
    @Provides
    fun entryUpdateIntervalAvailability(queue: LibraryUpdateQueue, build: AppBuild): EntryUpdateIntervalAvailability =
        EntryUpdateIntervalAvailability(isOffered = queue.isOffered && !build.hidesUnfinishedFeatures)

    @Provides
    fun releasePeriodFilterAvailability(queue: LibraryUpdateQueue, build: AppBuild): ReleasePeriodFilterAvailability =
        ReleasePeriodFilterAvailability(isOffered = queue.isOffered && !build.hidesUnfinishedFeatures)
}
