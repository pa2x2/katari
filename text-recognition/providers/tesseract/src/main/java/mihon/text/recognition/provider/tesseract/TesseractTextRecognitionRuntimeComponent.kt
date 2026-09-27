package mihon.text.recognition.provider.tesseract

import android.app.Application
import mihon.feature.runtime.application.ApplicationFeatureRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeComponent
import mihon.text.recognition.runtime.component.TextRecognitionRuntimeContribution
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

val tesseractTextRecognitionRuntimeComponent: ApplicationFeatureRuntimeComponent =
    object : TextRecognitionRuntimeComponent {
        override fun contribute(application: Application): TextRecognitionRuntimeContribution =
            TextRecognitionRuntimeContribution(
                providers = listOf(
                    TextRecognitionProviderContribution(
                        provider = TesseractTextRecognitionCatalog.provider,
                        components = listOf(TesseractTextRecognitionCatalog.recognizer),
                        implementations = listOf(TesseractRecognizer()),
                        presets = TesseractTextRecognitionCatalog.presets,
                        order = TESSERACT_PROVIDER_ORDER,
                    ),
                ),
            )
    }

private const val TESSERACT_PROVIDER_ORDER = 200
