package mihon.text.recognition.provider.mlkit.catalog

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.spi.contribution.TextRecognitionProviderContribution

/** Lists ML Kit in builds that exclude Google Play services, so settings can explain why it cannot be chosen. */
@ContributesTo(AppScope::class)
@BindingContainer
object MlKitCatalogTextRecognitionBindings {
    @Provides
    @IntoSet
    fun provider(): TextRecognitionProviderContribution = TextRecognitionProviderContribution(
        provider = MlKitTextRecognitionCatalog.provider(
            TextRecognitionBuildAvailability.NotIncluded(
                "Not included in FOSS builds because it needs Google Play services.",
            ),
        ),
        components = listOf(MlKitTextRecognitionCatalog.recognizer),
        presets = MlKitTextRecognitionCatalog.presets,
        order = MlKitTextRecognitionCatalog.PROVIDER_ORDER,
    )
}
