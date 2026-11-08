package app.katari.ui.di

import app.katari.core.image.progressive.ProgressiveImageEngine
import app.katari.core.image.progressive.ProgressiveImagePreferences
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

/** The one progressive image engine that page loaders and previews open their sessions on. */
@ContributesTo(AppScope::class)
@BindingContainer
object ProgressiveImageBindings {
    @Provides
    @SingleIn(AppScope::class)
    fun progressiveImageEngine(preferences: ProgressiveImagePreferences): ProgressiveImageEngine =
        ProgressiveImageEngine(preferences)
}
