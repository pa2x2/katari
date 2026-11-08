package app.katari.language.runtime.identification

import android.app.Application
import app.katari.language.api.identification.TextLanguageDetector
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ElementsIntoSet
import dev.zacsweers.metro.Provides

/** Android's text classifier, where the device has one. */
@ContributesTo(AppScope::class)
@BindingContainer
object PlatformTextLanguageDetectors {
    @Provides
    @ElementsIntoSet
    fun textClassifier(application: Application): Set<TextLanguageDetector> =
        setOfNotNull(androidTextClassifierLanguageDetector(application))
}
