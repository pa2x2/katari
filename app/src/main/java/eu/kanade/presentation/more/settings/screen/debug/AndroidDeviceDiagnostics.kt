package eu.kanade.presentation.more.settings.screen.debug

import android.os.Build
import androidx.profileinstaller.ProfileVerifier
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.DeviceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext
import mihon.core.platform.PlatformContext
import tachiyomi.domain.storage.DocumentUri

/** Logcat, the device's build facts, ART's profile verification and WorkManager's work list. */
@Inject
class AndroidDeviceDiagnostics(private val crashLogUtilFactory: CrashLogUtil.Factory) : DeviceDiagnostics {
    override val savesLogsToPickedDocument: Boolean = false

    override suspend fun dumpLogs(context: PlatformContext, exception: Throwable?) {
        crashLogUtilFactory.create(context).dumpLogs(exception)
    }

    override suspend fun dumpLogsToDocument(document: DocumentUri, exception: Throwable?) {
        error("Android shares dumped logs instead")
    }

    override suspend fun debugInfo(context: PlatformContext): String =
        // Loads the WebView package's label through the package manager
        withContext(Dispatchers.IO) { crashLogUtilFactory.create(context).getDebugInfo() }

    override val reportsProfileCompilation: Boolean = true

    override suspend fun profileCompilationStatus(): String {
        val result = ProfileVerifier.getCompilationStatusAsync().await().profileInstallResultCode
        return when (result) {
            ProfileVerifier.CompilationStatus.RESULT_CODE_NO_PROFILE_INSTALLED -> "No profile installed"
            ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE -> "Compiled"
            ProfileVerifier.CompilationStatus.RESULT_CODE_COMPILED_WITH_PROFILE_NON_MATCHING ->
                "Compiled non-matching"
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CACHE_FILE_EXISTS_BUT_CANNOT_BE_READ,
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_CANT_WRITE_PROFILE_VERIFICATION_RESULT_CACHE_FILE,
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_PACKAGE_NAME_DOES_NOT_EXIST,
            -> "Error $result"
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_UNSUPPORTED_API_VERSION -> "Not supported"
            ProfileVerifier.CompilationStatus.RESULT_CODE_PROFILE_ENQUEUED_FOR_COMPILATION -> "Pending compilation"
            ProfileVerifier.CompilationStatus.RESULT_CODE_ERROR_NO_PROFILE_EMBEDDED -> "No profile embedded"
            else -> "Unknown code $result"
        }
    }

    override fun deviceFacts(): List<DeviceFact> = buildList {
        add(DeviceFact(title = "Model", value = "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})"))

        if (DeviceUtil.oneUiVersion != null) {
            add(DeviceFact(title = "OneUI version", value = "${DeviceUtil.oneUiVersion}"))
        } else if (DeviceUtil.miuiMajorVersion != null) {
            add(DeviceFact(title = "MIUI version", value = "${DeviceUtil.miuiMajorVersion}"))
        }

        val androidVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Build.VERSION.RELEASE_OR_PREVIEW_DISPLAY
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Build.VERSION.RELEASE_OR_CODENAME
        } else {
            Build.VERSION.RELEASE
        }
        add(DeviceFact(title = "Android version", value = "$androidVersion (${Build.DISPLAY})"))
    }

    override val listsBackgroundWork: Boolean = true

    override fun backgroundWorkScreen(): Screen = WorkerInfoScreen()
}
