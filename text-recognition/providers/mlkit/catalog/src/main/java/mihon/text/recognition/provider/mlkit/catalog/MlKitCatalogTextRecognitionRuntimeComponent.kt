package mihon.text.recognition.provider.mlkit.catalog

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

/** Lists ML Kit in builds that exclude Google Play services, so settings can explain why it cannot be chosen. */
val mlKitCatalogTextRecognitionRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TextRecognitionRuntimeComponent {
        override fun contribute(application: Application): TextRecognitionRuntimeContribution =
            TextRecognitionRuntimeContribution(
                providers = listOf(
                    TextRecognitionProviderContribution(
                        provider = MlKitTextRecognitionCatalog.provider(
                            TextRecognitionBuildAvailability.NotIncluded(
                                "Not included in FOSS builds because it needs Google Play services.",
                            ),
                        ),
                        components = listOf(MlKitTextRecognitionCatalog.recognizer),
                        presets = MlKitTextRecognitionCatalog.presets,
                        order = MlKitTextRecognitionCatalog.PROVIDER_ORDER,
                    ),
                ),
            )
    }
