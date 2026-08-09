package mihon.tts.provider.android.setup

import android.app.Application
import android.content.Intent
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    private val blockingDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) : TtsEngineSetup {
    override val supportsSetup: Boolean = installIntent().resolveActivity(application.packageManager) != null

    override suspend fun acknowledge(disclosure: TtsProviderDisclosure) = Unit

    override suspend fun openSetup(): TtsSetupResult = launchInstall()

    override suspend fun installVoiceData(languages: Set<LanguageTag>): TtsSetupResult {
        if (languages.isEmpty()) return TtsSetupResult.Failed("At least one language is required")
        return launchInstall()
    }

    private suspend fun launchInstall(): TtsSetupResult {
        val intent = installIntent()
        val available = withContext(blockingDispatcher) {
            intent.resolveActivity(application.packageManager) != null
        }
        if (!available) {
            return TtsSetupResult.SettingsUnavailable
        }
        return withContext(mainDispatcher) {
            try {
                application.startActivity(intent)
                TtsSetupResult.Opened(TtsSetupDestination.External)
            } catch (_: RuntimeException) {
                TtsSetupResult.SettingsUnavailable
            }
        }
    }

    private fun installIntent(): Intent {
        return Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            .setPackage(enginePackage)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
