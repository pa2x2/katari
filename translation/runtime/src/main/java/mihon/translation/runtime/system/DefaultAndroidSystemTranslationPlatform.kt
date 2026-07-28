package mihon.translation.runtime.system

import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import java.util.Locale

internal class DefaultAndroidSystemTranslationPlatform(
    private val sdkInt: Int,
    private val bridge: AndroidTranslationManagerBridge?,
) : AndroidSystemTranslationPlatform {
    override suspend fun inspectDevice(): AndroidSystemDeviceInspection {
        if (sdkInt < Build.VERSION_CODES.S) return AndroidSystemDeviceInspection.UnsupportedOs
        if (bridge == null) return AndroidSystemDeviceInspection.ServiceMissing
        return AndroidSystemDeviceInspection.Available
    }

    override suspend fun inspect(pair: AndroidSystemTranslationPair): AndroidSystemTranslationInspection {
        if (sdkInt < Build.VERSION_CODES.S) return AndroidSystemTranslationInspection.UnsupportedOs
        val manager = bridge ?: return AndroidSystemTranslationInspection.ServiceMissing
        return try {
            manager.inspect(pair)
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            AndroidSystemTranslationInspection.Failed(
                "Android translation capabilities could not be read",
            )
        }
    }

    override suspend fun translate(
        pair: AndroidSystemTranslationPair,
        text: String,
    ): AndroidSystemPlatformExecution {
        if (sdkInt < Build.VERSION_CODES.S) {
            return AndroidSystemPlatformExecution.CapabilityChanged(
                AndroidSystemTranslationInspection.UnsupportedOs,
            )
        }
        val manager = bridge ?: return AndroidSystemPlatformExecution.CapabilityChanged(
            AndroidSystemTranslationInspection.ServiceMissing,
        )

        var translator: AndroidTranslationManagerTranslator? = null
        var registration: AndroidTranslationCapabilityRegistration? = null
        val cancellation = manager.createCancellation()
        return try {
            val selected = manager.selectCapability(pair)
                ?: return AndroidSystemPlatformExecution.CapabilityChanged(
                    AndroidSystemTranslationInspection.UnsupportedPair,
                )
            if (selected.state != AndroidSystemCapabilityState.OnDevice) {
                return AndroidSystemPlatformExecution.CapabilityChanged(
                    manager.toInspection(selected),
                )
            }

            val result = CompletableDeferred<AndroidSystemPlatformExecution>()
            registration = manager.observeCapabilities { update ->
                if (
                    update.samePairAs(selected) &&
                    update.state != AndroidSystemCapabilityState.OnDevice
                ) {
                    result.complete(
                        AndroidSystemPlatformExecution.CapabilityChanged(
                            manager.toInspection(update),
                        ),
                    )
                    cancellation.cancel()
                }
            }
            translator = manager.createTranslator(
                selected.sourceLanguageTag,
                selected.targetLanguageTag,
            )
            if (translator == null) {
                return AndroidSystemPlatformExecution.Failed(
                    "Android translation service could not create a translator",
                )
            }
            if (result.isCompleted) return result.await()

            val activeTranslator = translator
            coroutineScope {
                val translation = async {
                    when (val translated = activeTranslator.translate(text, cancellation)) {
                        is AndroidTranslationManagerResult.Success ->
                            AndroidSystemPlatformExecution.Success(translated.translatedText)
                        AndroidTranslationManagerResult.ContextUnsupported ->
                            AndroidSystemPlatformExecution.ContextUnsupported
                        is AndroidTranslationManagerResult.Failed ->
                            AndroidSystemPlatformExecution.Failed(translated.reason)
                    }
                }
                select {
                    result.onAwait { changed ->
                        translation.cancel()
                        changed
                    }
                    translation.onAwait { translated ->
                        translated
                    }
                }
            }
        } catch (error: CancellationException) {
            cancellation.cancel()
            throw error
        } catch (_: RuntimeException) {
            AndroidSystemPlatformExecution.Failed("Android translation failed")
        } finally {
            try {
                registration?.close()
            } catch (_: RuntimeException) {
                // The execution outcome remains authoritative after best-effort provider cleanup.
            }
            try {
                translator?.destroy()
            } catch (_: RuntimeException) {
                // The execution outcome remains authoritative after best-effort provider cleanup.
            }
        }
    }

    override suspend fun openSettings(): AndroidSystemPlatformSetup {
        if (sdkInt < Build.VERSION_CODES.S) return AndroidSystemPlatformSetup.SettingsUnavailable
        val manager = bridge ?: return AndroidSystemPlatformSetup.ServiceMissing
        return try {
            manager.openSettings()
        } catch (error: CancellationException) {
            throw error
        } catch (_: RuntimeException) {
            AndroidSystemPlatformSetup.Failed("Android translation settings could not be opened")
        }
    }

    private suspend fun AndroidTranslationManagerBridge.inspect(
        pair: AndroidSystemTranslationPair,
    ): AndroidSystemTranslationInspection {
        val selected = selectCapability(pair) ?: return AndroidSystemTranslationInspection.UnsupportedPair
        return toInspection(selected)
    }

    private suspend fun AndroidTranslationManagerBridge.selectCapability(
        pair: AndroidSystemTranslationPair,
    ): AndroidTranslationManagerCapability? {
        val capabilities = capabilities()
        return capabilities.firstOrNull { capability ->
            capability.sourceLanguageTag.equals(pair.source.value, ignoreCase = true) &&
                capability.targetLanguageTag.equals(pair.target.value, ignoreCase = true)
        } ?: capabilities.firstOrNull { capability ->
            capability.sourceLanguageTag.matchesRequested(pair.source.value) &&
                capability.targetLanguageTag.matchesRequested(pair.target.value)
        }
    }

    private fun AndroidTranslationManagerBridge.toInspection(
        capability: AndroidTranslationManagerCapability,
    ): AndroidSystemTranslationInspection {
        return AndroidSystemTranslationInspection.Capability(capability.state)
    }

    private fun AndroidTranslationManagerCapability.samePairAs(
        other: AndroidTranslationManagerCapability,
    ): Boolean {
        return sourceLanguageTag.equals(other.sourceLanguageTag, ignoreCase = true) &&
            targetLanguageTag.equals(other.targetLanguageTag, ignoreCase = true)
    }

    private fun String.matchesRequested(requestedTag: String): Boolean {
        if (equals(requestedTag, ignoreCase = true)) return true
        val offered = Locale.forLanguageTag(this)
        val requested = Locale.forLanguageTag(requestedTag)
        return offered.language.equals(requested.language, ignoreCase = true) &&
            offered.script.isEmpty() &&
            offered.country.isEmpty()
    }
}
