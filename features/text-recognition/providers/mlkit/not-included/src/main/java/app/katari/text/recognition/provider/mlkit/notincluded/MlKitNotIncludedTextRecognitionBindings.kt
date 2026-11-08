package app.katari.text.recognition.provider.mlkit.notincluded

import app.katari.text.recognition.api.provider.TextRecognitionBuildAvailability
import app.katari.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionCatalog
import app.katari.text.recognition.spi.contribution.TextRecognitionProviderContribution
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides

/** Lists ML Kit in builds that exclude Google Play services, so settings can explain why it cannot be chosen. */
@ContributesTo(AppScope::class)
@BindingContainer
object MlKitNotIncludedTextRecognitionBindings {
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
