package mihon.app.ui.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import mihon.core.image.progressive.ProgressiveImageEngine
import mihon.core.image.progressive.ProgressiveImagePreferences

/** The one progressive image engine that page loaders and previews open their sessions on. */
@ContributesTo(AppScope::class)
@BindingContainer
object ProgressiveImageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun progressiveImageEngine(preferences: ProgressiveImagePreferences): ProgressiveImageEngine =
        ProgressiveImageEngine(preferences)
}
