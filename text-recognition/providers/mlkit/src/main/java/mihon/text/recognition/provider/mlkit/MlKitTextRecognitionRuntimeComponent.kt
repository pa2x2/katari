package mihon.text.recognition.provider.mlkit

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionCatalog
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

val mlKitTextRecognitionRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TextRecognitionRuntimeComponent {
        override fun contribute(application: Application): TextRecognitionRuntimeContribution {
            val recognizers = MlKitScriptRecognizers()
            return TextRecognitionRuntimeContribution(
                providers = listOf(
                    TextRecognitionProviderContribution(
                        provider = MlKitTextRecognitionCatalog.provider(TextRecognitionBuildAvailability.Included),
                        components = listOf(MlKitTextRecognitionCatalog.recognizer),
                        implementations = listOf(
                            MlKitRecognizer(recognizers, MlKitScriptModules(application, recognizers)),
                        ),
                        presets = MlKitTextRecognitionCatalog.presets,
                        order = MlKitTextRecognitionCatalog.PROVIDER_ORDER,
                    ),
                ),
            )
        }
    }
