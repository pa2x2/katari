package mihon.language.runtime.identification

import android.app.Application
import mihon.language.api.identification.TextLanguageDetector

/** Platform-owned language detectors available to any application feature. */
fun createPlatformTextLanguageDetectors(application: Application): List<TextLanguageDetector> {
    return listOfNotNull(androidTextClassifierLanguageDetector(application))
}
