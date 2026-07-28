package mihon.translation.runtime.system

import android.app.Application
import android.app.PendingIntent
import android.icu.util.ULocale
import android.os.Build
import android.os.CancellationSignal
import android.view.translation.TranslationCapability
import android.view.translation.TranslationContext
import android.view.translation.TranslationManager
import android.view.translation.TranslationRequest
import android.view.translation.TranslationRequestValue
import android.view.translation.TranslationResponse
import android.view.translation.TranslationResponseValue
import android.view.translation.TranslationSpec
import android.view.translation.Translator
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor
import java.util.function.Consumer

internal data class AndroidTranslationManagerCapability(
    val sourceLanguageTag: String,
    val targetLanguageTag: String,
    val state: AndroidSystemCapabilityState,
)

internal fun interface AndroidTranslationCapabilityRegistration {
    fun close()
}

internal fun interface AndroidTranslationCancellation {
    fun cancel()
}

internal sealed interface AndroidTranslationManagerResult {
    data class Success(
        val translatedText: String,
    ) : AndroidTranslationManagerResult

    data object ContextUnsupported : AndroidTranslationManagerResult

    data class Failed(
        val reason: String,
    ) : AndroidTranslationManagerResult
}

internal interface AndroidTranslationManagerTranslator {
    suspend fun translate(
        text: String,
        cancellation: AndroidTranslationCancellation,
    ): AndroidTranslationManagerResult

    fun destroy()
}

internal interface AndroidTranslationManagerBridge {
    suspend fun capabilities(): List<AndroidTranslationManagerCapability>

    fun settingsAvailable(): Boolean

    suspend fun openSettings(): AndroidSystemPlatformSetup

    fun observeCapabilities(
        listener: (AndroidTranslationManagerCapability) -> Unit,
    ): AndroidTranslationCapabilityRegistration

    fun createCancellation(): AndroidTranslationCancellation

    suspend fun createTranslator(
        sourceLanguageTag: String,
        targetLanguageTag: String,
    ): AndroidTranslationManagerTranslator?
}

internal fun createAndroidTranslationManagerBridge(
    application: Application,
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
): AndroidTranslationManagerBridge? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val manager = application.getSystemService(TranslationManager::class.java) ?: return null
    return FrameworkAndroidTranslationManagerBridge(manager, workerDispatcher)
}

@RequiresApi(Build.VERSION_CODES.S)
private class FrameworkAndroidTranslationManagerBridge(
    private val manager: TranslationManager,
    private val workerDispatcher: CoroutineDispatcher,
) : AndroidTranslationManagerBridge {
    override suspend fun capabilities(): List<AndroidTranslationManagerCapability> {
        return withContext(workerDispatcher) {
            manager.getOnDeviceTranslationCapabilities(
                TranslationSpec.DATA_FORMAT_TEXT,
                TranslationSpec.DATA_FORMAT_TEXT,
            ).map(::mapCapability)
        }
    }

    override fun settingsAvailable(): Boolean {
        return manager.onDeviceTranslationSettingsActivityIntent != null
    }

    override suspend fun openSettings(): AndroidSystemPlatformSetup {
        val intent = manager.onDeviceTranslationSettingsActivityIntent
            ?: return AndroidSystemPlatformSetup.SettingsUnavailable
        return try {
            intent.send()
            AndroidSystemPlatformSetup.Opened
        } catch (_: PendingIntent.CanceledException) {
            AndroidSystemPlatformSetup.Failed("Android translation settings are no longer available")
        } catch (_: RuntimeException) {
            AndroidSystemPlatformSetup.Failed("Android translation settings could not be opened")
        }
    }

    override fun observeCapabilities(
        listener: (AndroidTranslationManagerCapability) -> Unit,
    ): AndroidTranslationCapabilityRegistration {
        val consumer = Consumer<TranslationCapability> { capability ->
            listener(mapCapability(capability))
        }
        manager.addOnDeviceTranslationCapabilityUpdateListener(DIRECT_EXECUTOR, consumer)
        return AndroidTranslationCapabilityRegistration {
            manager.removeOnDeviceTranslationCapabilityUpdateListener(consumer)
        }
    }

    override fun createCancellation(): AndroidTranslationCancellation {
        return FrameworkAndroidTranslationCancellation(CancellationSignal())
    }

    override suspend fun createTranslator(
        sourceLanguageTag: String,
        targetLanguageTag: String,
    ): AndroidTranslationManagerTranslator? {
        val context = TranslationContext.Builder(
            TranslationSpec(ULocale.forLanguageTag(sourceLanguageTag), TranslationSpec.DATA_FORMAT_TEXT),
            TranslationSpec(ULocale.forLanguageTag(targetLanguageTag), TranslationSpec.DATA_FORMAT_TEXT),
        ).build()
        return suspendCancellableCoroutine { continuation ->
            manager.createOnDeviceTranslator(context, DIRECT_EXECUTOR) { translator ->
                if (continuation.isActive) {
                    continuation.resume(
                        translator?.let(::FrameworkAndroidTranslationManagerTranslator),
                    ) { _, value, _ ->
                        value?.destroy()
                    }
                } else {
                    translator?.destroy()
                }
            }
        }
    }

    private fun mapCapability(capability: TranslationCapability): AndroidTranslationManagerCapability {
        return AndroidTranslationManagerCapability(
            sourceLanguageTag = capability.sourceSpec.locale.toLanguageTag(),
            targetLanguageTag = capability.targetSpec.locale.toLanguageTag(),
            state = when (capability.state) {
                TranslationCapability.STATE_ON_DEVICE -> AndroidSystemCapabilityState.OnDevice
                TranslationCapability.STATE_AVAILABLE_TO_DOWNLOAD ->
                    AndroidSystemCapabilityState.AvailableToDownload
                TranslationCapability.STATE_DOWNLOADING -> AndroidSystemCapabilityState.Downloading
                else -> AndroidSystemCapabilityState.Unavailable
            },
        )
    }

    private companion object {
        val DIRECT_EXECUTOR = Executor(Runnable::run)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class FrameworkAndroidTranslationManagerTranslator(
    private val translator: Translator,
) : AndroidTranslationManagerTranslator {
    override suspend fun translate(
        text: String,
        cancellation: AndroidTranslationCancellation,
    ): AndroidTranslationManagerResult {
        require(cancellation is FrameworkAndroidTranslationCancellation)
        val request = TranslationRequest.Builder()
            .setFlags(TranslationRequest.FLAG_TRANSLATION_RESULT)
            .setTranslationRequestValues(listOf(TranslationRequestValue.forText(text)))
            .build()
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancellation.cancel() }
            translator.translate(request, cancellation.signal, DIRECT_EXECUTOR) { response ->
                if (continuation.isActive) {
                    continuation.resume(response.toManagerResult()) { _, _, _ -> }
                }
            }
        }
    }

    override fun destroy() {
        translator.destroy()
    }

    private fun TranslationResponse.toManagerResult(): AndroidTranslationManagerResult {
        if (translationStatus == TranslationResponse.TRANSLATION_STATUS_CONTEXT_UNSUPPORTED) {
            return AndroidTranslationManagerResult.ContextUnsupported
        }
        if (translationStatus != TranslationResponse.TRANSLATION_STATUS_SUCCESS) {
            return AndroidTranslationManagerResult.Failed("Android translation failed")
        }
        val value = translationResponseValues[0]
            ?: return AndroidTranslationManagerResult.Failed("Android translation returned no result")
        if (value.statusCode != TranslationResponseValue.STATUS_SUCCESS) {
            return AndroidTranslationManagerResult.Failed("Android translation returned an error")
        }
        val translatedText = value.text?.toString()
        return if (translatedText.isNullOrBlank()) {
            AndroidTranslationManagerResult.Failed("Android translation returned an empty result")
        } else {
            AndroidTranslationManagerResult.Success(translatedText)
        }
    }

    private companion object {
        val DIRECT_EXECUTOR = Executor(Runnable::run)
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private class FrameworkAndroidTranslationCancellation(
    val signal: CancellationSignal,
) : AndroidTranslationCancellation {
    override fun cancel() {
        signal.cancel()
    }
}
