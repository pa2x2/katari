package eu.kanade.presentation.util

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds

/** A platform's own words for source errors that only it recognises, which [SourceErrorMessages] asks first. */
fun interface SourceErrorExplanation {
    /** Null for an error it doesn't recognise. */
    fun explain(context: PlatformContext, error: Throwable): String?
}

@ContributesTo(AppScope::class)
@BindingContainer
interface SourceErrorExplanationBindings {
    // Android has none of its own
    @Multibinds(allowEmpty = true)
    fun sourceErrorExplanations(): Set<SourceErrorExplanation>
}
