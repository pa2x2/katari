package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import eu.kanade.tachiyomi.ui.library.ReleasePeriodFilterAvailability
import eu.kanade.tachiyomi.util.system.isReleaseBuildType

@BindingContainer
object LibraryBindings {
    // TODO: re-enable when custom intervals are ready for stable
    @Provides
    fun releasePeriodFilterAvailability(): ReleasePeriodFilterAvailability =
        ReleasePeriodFilterAvailability(isOffered = !isReleaseBuildType)
}
