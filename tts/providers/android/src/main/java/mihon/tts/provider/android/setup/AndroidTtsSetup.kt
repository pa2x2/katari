package mihon.tts.provider.android.setup

import android.app.Application
import android.content.Intent
import android.speech.tts.TextToSpeech
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.host.TtsSetupDestination
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.spi.setup.TtsEngineSetup
import mihon.tts.spi.setup.TtsSetupResult

internal class AndroidTtsSetup(
    private val application: Application,
    private val enginePackage: String,
    override val engine: TtsEngineId,
) : TtsEngineSetup {
    override val supportsSetup: Boolean
        get() = installIntent().resolveActivity(application.packageManager) != null

    override suspend fun acknowledge(disclosure: TtsProviderDisclosure) = Unit

    override suspend fun openSetup(): TtsSetupResult = launchInstall()

    override suspend fun installVoiceData(languages: Set<LanguageTag>): TtsSetupResult {
        if (languages.isEmpty()) return TtsSetupResult.Failed("At least one language is required")
        return launchInstall()
    }

    private fun launchInstall(): TtsSetupResult {
        val intent = installIntent()
        if (intent.resolveActivity(application.packageManager) == null) {
            return TtsSetupResult.SettingsUnavailable
        }
        return try {
            application.startActivity(intent)
            TtsSetupResult.Opened(TtsSetupDestination.External)
        } catch (_: RuntimeException) {
            TtsSetupResult.SettingsUnavailable
        }
    }

    private fun installIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            .setPackage(enginePackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
