package mihon.translation.runtime

import android.app.Application
import android.view.textclassifier.TextClassificationManager
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import mihon.feature.graph.ApplicableFeatureIntegration
import mihon.feature.runtime.application.ApplicationFeatureRuntimeDependencies
import mihon.feature.runtime.application.ApplicationFeatureRuntimeInstallationContext
import mihon.feature.runtime.application.installApplicationFeatureRuntimeModules
import mihon.feature.runtime.application.validateInstalledApplicationFeatureRuntimeModules
import mihon.feature.runtime.createFeatureRuntimeComposition
import mihon.translation.runtime.di.translationFeatureRuntimeModule
import mihon.translation.runtime.graph.TranslationFeatureGraphStateValidator
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.InjektScope
import uy.kohesive.injekt.registry.default.DefaultRegistrar

class TranslationFeatureGraphTest {
    @Test
    fun `runtime module makes Translation applicable to the application subject`() {
        val previousInjekt = Injekt
        try {
            Injekt = InjektScope(DefaultRegistrar())
            val application = mockk<Application>(relaxed = true) {
                every { getSystemService(TextClassificationManager::class.java) } returns null
            }
            val installation = installApplicationFeatureRuntimeModules(
                registrar = Injekt,
                modules = listOf(translationFeatureRuntimeModule),
                context = ApplicationFeatureRuntimeInstallationContext(
                    application = application,
                    dependencies = ApplicationFeatureRuntimeDependencies(
                        profilePreferenceOwners = ProfilePreferenceOwnerInstaller(
                            owners = ProfilePreferenceOwnerRegistry(),
                            preferenceStore = ::InMemoryPreferenceStore,
                        ),
                    ),
                ),
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
