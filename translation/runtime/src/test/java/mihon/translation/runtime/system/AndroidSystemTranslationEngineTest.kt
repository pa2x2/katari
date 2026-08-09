package mihon.translation.runtime.system

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.host.TranslationSetupDestination
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.api.request.ResolvedTranslationRequest
import mihon.translation.spi.engine.TranslationEngineDeviceAvailability
import mihon.translation.spi.engine.TranslationEngineExecution
import mihon.translation.spi.engine.TranslationEnginePreparation
import mihon.translation.spi.setup.TranslationSetupResult
import org.junit.jupiter.api.Test

class AndroidSystemTranslationEngineTest {
    @Test
    fun `device inspection does not require text or a language pair`() = runTest {
        val platform = FakePlatform(onDevice())
        val engine = AndroidSystemTranslationEngine(platform)

        engine.inspectDevice() shouldBe TranslationEngineDeviceAvailability.Available
        platform.pairInspectionCount shouldBe 0
    }

    @Test
    fun `platform availability maps to precise preparation states`() = runTest {
        val cases = listOf(
            AndroidSystemTranslationInspection.UnsupportedOs to
                TranslationEnginePreparation.Unavailable(TranslationUnavailableReason.UnsupportedOs(31)),
            AndroidSystemTranslationInspection.ServiceMissing to
                TranslationEnginePreparation.Unavailable(TranslationUnavailableReason.ServiceMissing),
            AndroidSystemTranslationInspection.UnsupportedPair to
                TranslationEnginePreparation.Unavailable(
                    TranslationUnavailableReason.UnsupportedLanguagePair(ENGLISH, POLISH),
                ),
            AndroidSystemTranslationInspection.Capability(
                AndroidSystemCapabilityState.AvailableToDownload,
            ) to TranslationEnginePreparation.SystemSetupRequired(
                TranslationSystemSetupReason.LanguageModelsRequired,
            ),
            AndroidSystemTranslationInspection.Capability(
                AndroidSystemCapabilityState.Downloading,
            ) to TranslationEnginePreparation.SetupInProgress(),
            AndroidSystemTranslationInspection.Capability(
                AndroidSystemCapabilityState.Unavailable,
            ) to TranslationEnginePreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(ENGLISH, POLISH),
            ),
        )

        cases.forEach { (inspection, expected) ->
            val platform = FakePlatform(inspection)
            AndroidSystemTranslationEngine(platform).prepare(REQUEST) shouldBe expected
        }
    }

    @Test
    fun `ready request keeps text and is revalidated before execution`() = runTest {
        val platform = FakePlatform(
            inspection = onDevice(),
            execution = AndroidSystemPlatformExecution.Success("Cześć"),
        )
        val engine = AndroidSystemTranslationEngine(platform)
        val ready = engine.prepare(REQUEST) as TranslationEnginePreparation.Ready

        engine.revalidate(ready.request) shouldBe ready
        engine.translate(ready.request) shouldBe TranslationEngineExecution.Success("Cześć")

        platform.translatedPair shouldBe AndroidSystemTranslationPair(ENGLISH, POLISH)
        platform.translatedText shouldBe "Hello"
    }

    @Test
    fun `capability change during execution returns the latest preparation`() = runTest {
        val platform = FakePlatform(
            inspection = onDevice(),
            execution = AndroidSystemPlatformExecution.CapabilityChanged(
                AndroidSystemTranslationInspection.Capability(
                    AndroidSystemCapabilityState.Downloading,
                ),
            ),
        )
        val engine = AndroidSystemTranslationEngine(platform)
        val ready = engine.prepare(REQUEST) as TranslationEnginePreparation.Ready

        engine.translate(ready.request) shouldBe TranslationEngineExecution.PreparationChanged(
            TranslationEnginePreparation.SetupInProgress(),
        )
    }

    @Test
    fun `system setup opens only through the platform result`() = runTest {
        val opened = AndroidSystemTranslationEngine(
            FakePlatform(onDevice(), setup = AndroidSystemPlatformSetup.Opened),
        )
        val missing = AndroidSystemTranslationEngine(
            FakePlatform(onDevice(), setup = AndroidSystemPlatformSetup.SettingsUnavailable),
        )

        opened.openSetup() shouldBe
            TranslationSetupResult.Opened(TranslationSetupDestination.External)
        missing.openSetup() shouldBe TranslationSetupResult.SettingsUnavailable
    }

    @Test
    fun `platform failures remain attributed to the Android engine`() = runTest {
        val engine = AndroidSystemTranslationEngine(
            FakePlatform(AndroidSystemTranslationInspection.Failed("OEM service failed")),
        )

        engine.prepare(REQUEST) shouldBe TranslationEnginePreparation.Unavailable(
            TranslationUnavailableReason.EngineUnavailable(
                TranslationEngineId("android-system"),
                "OEM service failed",
            ),
        )
    }

    private class FakePlatform(
        var inspection: AndroidSystemTranslationInspection,
        var execution: AndroidSystemPlatformExecution =
            AndroidSystemPlatformExecution.Failed("Translation was not configured"),
        var setup: AndroidSystemPlatformSetup = AndroidSystemPlatformSetup.SettingsUnavailable,
    ) : AndroidSystemTranslationPlatform {
        var translatedPair: AndroidSystemTranslationPair? = null
        var translatedText: String? = null
        var pairInspectionCount = 0

        override suspend fun inspectDevice(): AndroidSystemDeviceInspection {
            return when (val current = inspection) {
                AndroidSystemTranslationInspection.UnsupportedOs -> AndroidSystemDeviceInspection.UnsupportedOs
                AndroidSystemTranslationInspection.ServiceMissing -> AndroidSystemDeviceInspection.ServiceMissing
                is AndroidSystemTranslationInspection.Failed -> AndroidSystemDeviceInspection.Failed(current.reason)
                else -> AndroidSystemDeviceInspection.Available
            }
        }

        override suspend fun inspectLanguageSupport() =
            TranslationLanguageSupportInspection.Available(
                TranslationLanguageSupport.AnyLanguage,
            )

        override suspend fun inspect(
            pair: AndroidSystemTranslationPair,
        ): AndroidSystemTranslationInspection {
            pairInspectionCount++
            return inspection
        }

        override suspend fun translate(
            pair: AndroidSystemTranslationPair,
            text: String,
        ): AndroidSystemPlatformExecution {
            translatedPair = pair
            translatedText = text
            return execution
        }

        override suspend fun openSettings(): AndroidSystemPlatformSetup = setup
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val POLISH = LanguageTag.require("pl")
        val REQUEST = ResolvedTranslationRequest(
            text = "Hello",
            sourceLanguage = ENGLISH,
            targetLanguage = POLISH,
            engine = TranslationEngineId("android-system"),
        )

        fun onDevice() = AndroidSystemTranslationInspection.Capability(
            AndroidSystemCapabilityState.OnDevice,
        )
    }
}
