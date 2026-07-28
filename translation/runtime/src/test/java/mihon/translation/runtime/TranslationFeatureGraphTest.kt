package mihon.translation.runtime

import android.app.Application
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.runtime.ApplicationFeatureRuntimeInstallationContext
import mihon.feature.runtime.createFeatureRuntimeComposition
import mihon.feature.runtime.installApplicationFeatureRuntimeModules
import mihon.feature.runtime.validateInstalledApplicationFeatureRuntimeModules
import org.junit.jupiter.api.Test
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class TranslationFeatureGraphTest {
    @Test
    fun `runtime module makes Translation applicable to the application subject`() {
        val previousInjekt = Injekt
        try {
            Injekt = InjektScope(DefaultRegistrar())
            val installation = installApplicationFeatureRuntimeModules(
                registrar = Injekt,
                modules = listOf(translationFeatureRuntimeModule),
                context = ApplicationFeatureRuntimeInstallationContext(mockk<Application>(relaxed = true)),
            )
            val composition = createFeatureRuntimeComposition(listOf(installation.featureRuntimeInputs))

            validateInstalledApplicationFeatureRuntimeModules(installation)
            TranslationFeatureGraphStateValidator(composition.evaluation).validate()
            composition.evaluation.integrations.single().shouldBeInstanceOf<ApplicableFeatureIntegration>()
        } finally {
            Injekt = previousInjekt
        }
    }
}
