package eu.kanade.tachiyomi.util

import android.content.Context
import android.os.Build
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.more.settings.screen.debug.AppDebugInfo
import eu.kanade.presentation.more.settings.screen.debug.LOG_DUMP_NAME
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.util.storage.getUriCompat
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import tachiyomi.core.common.util.lang.withNonCancellableContext
import tachiyomi.core.common.util.lang.withUIContext

@AssistedInject
class CrashLogUtil(
    @Assisted private val context: Context,
    private val appDebugInfo: AppDebugInfo,
    private val networkPreferences: NetworkPreferences,
) {
    @AssistedFactory
    fun interface Factory {
        fun create(context: Context): CrashLogUtil
    }

    suspend fun dumpLogs(exception: Throwable? = null) = withNonCancellableContext {
        try {
            val file = context.createFileInCacheDir(LOG_DUMP_NAME)

            file.appendText(getDebugInfo() + "\n\n")
            appDebugInfo.problematicExtensions()?.let { file.appendText("$it\n\n") }
            exception?.let { file.appendText("$it\n\n") }

            val logPriority = if (networkPreferences.verboseLogging.get()) "V" else "E"
            Runtime.getRuntime().exec("logcat *:$logPriority -d -v year -v zone -f ${file.absolutePath}").waitFor()

            val uri = file.getUriCompat(context)
            context.startActivity(uri.toShareIntent(context, "text/plain"))
        } catch (_: Throwable) {
            withUIContext { context.toast("Failed to get logs") }
        }
    }

    fun getDebugInfo(): String = appDebugInfo.summary(
        deviceLines = listOf(
            "Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}; build ${Build.DISPLAY})",
            "Device brand: ${Build.BRAND}",
            "Device manufacturer: ${Build.MANUFACTURER}",
            "Device name: ${Build.DEVICE} (${Build.PRODUCT})",
            "Device model: ${Build.MODEL}",
            "WebView: ${WebViewUtil.getVersion(context)}",
        ),
    )
}
