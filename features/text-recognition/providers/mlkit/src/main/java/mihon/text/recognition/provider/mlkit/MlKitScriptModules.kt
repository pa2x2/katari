package mihon.text.recognition.provider.mlkit

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import mihon.text.recognition.provider.mlkit.catalog.MlKitTextRecognitionScript
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognitionPlatformModelsInstallation
import java.util.concurrent.ConcurrentHashMap

/**
 * The script models Google Play services downloads for ML Kit. Nothing is downloaded until [install] runs, which
 * hosts call only after the user approved the download.
 */
internal class MlKitScriptModules(
    private val context: Context,
    private val recognizers: MlKitScriptRecognizers,
) {
    private val client by lazy { ModuleInstall.getClient(context) }

    /** Scripts known to be installed; Play services does not remove optional modules an app still uses. */
    private val installed = ConcurrentHashMap.newKeySet<MlKitTextRecognitionScript>()

    suspend fun availability(script: MlKitTextRecognitionScript): TextRecognitionComponentAvailability {
        if (script in installed) return TextRecognitionComponentAvailability.Available
        val services = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (services != ConnectionResult.SUCCESS) {
            return TextRecognitionComponentAvailability.Unavailable(
                "Google Play services is not available on this device.",
            )
        }
        val available = try {
            client.areModulesAvailable(recognizers[script]).await().areModulesAvailable()
        } catch (error: ApiException) {
            return TextRecognitionComponentAvailability.Unavailable(
                "Google Play services could not check ML Kit's models (${error.statusCode}).",
            )
        }
        if (!available) {
            return TextRecognitionComponentAvailability.PlatformModelsRequired(
                "Google Play services must download ML Kit's ${script.displayName} model.",
            )
        }
        installed += script
        return TextRecognitionComponentAvailability.Available
    }

    suspend fun install(script: MlKitTextRecognitionScript): TextRecognitionPlatformModelsInstallation {
        val finished = CompletableDeferred<TextRecognitionPlatformModelsInstallation>()
        val listener = InstallStatusListener { update ->
            when (update.installState) {
                InstallState.STATE_COMPLETED -> finished.complete(TextRecognitionPlatformModelsInstallation.Installed)
                InstallState.STATE_FAILED -> finished.complete(
                    TextRecognitionPlatformModelsInstallation.Failed(
                        "Google Play services could not download the model (${update.errorCode}).",
                    ),
                )
                InstallState.STATE_CANCELED -> finished.complete(
                    TextRecognitionPlatformModelsInstallation.Failed("The model download was canceled."),
                )
            }
        }
        val request = ModuleInstallRequest.newBuilder()
            .addApi(recognizers[script])
            .setListener(listener)
            .build()
        return try {
            val response = client.installModules(request).await()
            val result = if (response.areModulesAlreadyInstalled()) {
                TextRecognitionPlatformModelsInstallation.Installed
            } else {
                finished.await()
            }
            if (result == TextRecognitionPlatformModelsInstallation.Installed) installed += script
            result
        } catch (error: ApiException) {
            TextRecognitionPlatformModelsInstallation.Failed(
                "Google Play services could not download the model (${error.statusCode}).",
            )
        } finally {
            client.unregisterListener(listener)
        }
    }
}
