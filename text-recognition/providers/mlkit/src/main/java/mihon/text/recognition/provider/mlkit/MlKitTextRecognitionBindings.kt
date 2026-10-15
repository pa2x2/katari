package mihon.text.recognition.provider.mlkit

import android.app.Application
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionCatalog
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

@ContributesTo(AppScope::class)
@BindingContainer
object MlKitTextRecognitionBindings {
    @Provides
    @IntoSet
    fun provider(application: Application): TextRecognitionProviderContribution {
        val recognizers = MlKitScriptRecognizers(application)
        return TextRecognitionProviderContribution(
            provider = MlKitTextRecognitionCatalog.provider(TextRecognitionBuildAvailability.Included),
            components = listOf(MlKitTextRecognitionCatalog.recognizer),
            implementations = listOf(
                MlKitRecognizer(recognizers, MlKitScriptModules(application, recognizers)),
            ),
            presets = MlKitTextRecognitionCatalog.presets,
            order = MlKitTextRecognitionCatalog.PROVIDER_ORDER,
        )
    }
}
