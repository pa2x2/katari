package mihon.tts.provider.android.discovery

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import mihon.tts.api.engine.KnownTtsEngine
import mihon.tts.api.engine.TtsEngineArtwork
import mihon.tts.api.engine.TtsEngineBuildAvailability
import mihon.tts.api.engine.TtsEngineDetails
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.provider.TtsProviderPresentation
import mihon.tts.provider.android.engine.AndroidTtsEngine
import mihon.tts.provider.android.setup.AndroidTtsSetup
import mihon.tts.spi.contribution.TtsEngineContribution
import java.util.Locale

internal val ANDROID_TTS_PROVIDER_ID = TtsProviderId("android-tts")

internal fun discoverAndroidTtsEngines(application: Application): List<TtsEngineContribution> {
    val packageManager = application.packageManager
    val defaultPackage = Settings.Secure.getString(
        application.contentResolver,
        Settings.Secure.TTS_DEFAULT_SYNTH,
    )
    return packageManager.queryTtsServices()
        .mapNotNull { service ->
            val packageName = service.serviceInfo?.packageName ?: return@mapNotNull null
            if (!packageName.isSupportedAndroidTtsEngine()) return@mapNotNull null
            val engineId = packageName.toAndroidTtsEngineId()
            val engineName = service.loadLabel(packageManager).toString().takeIf(String::isNotBlank)
                ?: packageName
            val descriptor = KnownTtsEngine(
                id = engineId,
                providerId = ANDROID_TTS_PROVIDER_ID,
                providerName = "Android text-to-speech",
                engineName = engineName,
                buildAvailability = TtsEngineBuildAvailability.Included,
                artwork = TtsEngineArtwork.InstalledApplication(
                    packageName = packageName,
                    fallbackResourceId = android.R.drawable.sym_def_app_icon,
                ),
                details = TtsEngineDetails(
                    description = "Speech synthesis through the installed $engineName app.",
                    processingDescription = "Processing depends on the selected Android voice.",
                    privacyDescription = "Selected text is passed only to the chosen Android TTS engine. " +
                        "Network voices are used only when the profile allows them.",
                ),
                documentationUrl = "https://developer.android.com/reference/android/speech/tts/TextToSpeech",
            )
            val presentation = TtsProviderPresentation(
                providerId = ANDROID_TTS_PROVIDER_ID,
                providerName = "Android text-to-speech",
                engineName = engineName,
                documentationUrl = descriptor.documentationUrl,
            )
            val engine = AndroidTtsEngine(
                application = application,
                enginePackage = packageName,
                catalogEntry = descriptor,
                presentation = presentation,
            )
            TtsEngineContribution(
                engine = engine,
                setup = AndroidTtsSetup(application, packageName, engineId),
                order = if (packageName == defaultPackage) DEFAULT_ENGINE_ORDER else INSTALLED_ENGINE_ORDER,
            )
        }
        .sortedWith(compareBy(TtsEngineContribution::order, { it.catalogEntry.engineName.lowercase(Locale.ROOT) }))
}

internal fun PackageManager.isAndroidTtsEngineInstalled(packageName: String): Boolean {
    return packageName.isSupportedAndroidTtsEngine() && queryTtsServices(packageName).isNotEmpty()
}

private fun PackageManager.queryTtsServices(packageName: String? = null): List<ResolveInfo> {
    val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).apply {
        `package` = packageName
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
    } else {
        @Suppress("DEPRECATION")
        queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
}

private fun String.toAndroidTtsEngineId(): TtsEngineId {
    val encodedPackage = encodeToByteArray().joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xff)
    }
    return TtsEngineId("android-$encodedPackage")
}

private fun String.isSupportedAndroidTtsEngine(): Boolean {
    return this != UNSUPPORTED_TRANSLATOR_PACKAGE
}

private const val DEFAULT_ENGINE_ORDER = 0
private const val INSTALLED_ENGINE_ORDER = 100
private const val UNSUPPORTED_TRANSLATOR_PACKAGE = "dev.davidv.translator"
