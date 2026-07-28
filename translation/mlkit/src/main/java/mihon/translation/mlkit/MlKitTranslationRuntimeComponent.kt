package mihon.translation.mlkit

import android.app.Application
import android.os.Build
import mihon.feature.runtime.ApplicationFeatureRuntimeComponent
import mihon.translation.runtime.TranslationRuntimeComponent
import mihon.translation.runtime.TranslationRuntimeContribution

public val mlKitTranslationRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TranslationRuntimeComponent {
        override fun contribute(application: Application): TranslationRuntimeContribution {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return TranslationRuntimeContribution()
            }
            return TranslationRuntimeContribution(
                sourceLanguageDetectors = listOf(createMlKitSourceLanguageDetector()),
            )
        }
    }
