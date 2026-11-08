package mihon.language.runtime.identification

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Multibinds
import mihon.language.api.identification.TextLanguageDetector

@ContributesTo(AppScope::class)
interface TextLanguageDetectors {
    /** The platform's own detectors, which features ask before guessing a text's language; a platform may have none. */
    @Multibinds(allowEmpty = true)
    fun detectors(): Set<TextLanguageDetector>
}
