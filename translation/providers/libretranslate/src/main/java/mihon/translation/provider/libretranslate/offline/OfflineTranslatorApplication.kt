package mihon.translation.provider.libretranslate.offline

import android.content.Context
import android.content.Intent
import android.net.Uri

internal interface OfflineTranslatorApp {
    fun isInstalled(): Boolean
    fun open(): Boolean
    fun openInstallationPage(): Boolean
}

internal class OfflineTranslatorApplication(
    private val context: Context,
) : OfflineTranslatorApp {
    override fun isInstalled(): Boolean = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME) != null

    override fun open(): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE_NAME)
            ?: return false
        return start(intent)
    }

    override fun openInstallationPage(): Boolean {
        return start(Intent(Intent.ACTION_VIEW, Uri.parse(INSTALLATION_URL)))
    }

    private fun start(intent: Intent): Boolean {
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }

    companion object {
        const val PACKAGE_NAME = "dev.davidv.translator"
        const val INSTALLATION_URL = "https://f-droid.org/packages/dev.davidv.translator/"
        const val DOCUMENTATION_URL = "https://github.com/DavidVentura/offline-translator"
    }
}
