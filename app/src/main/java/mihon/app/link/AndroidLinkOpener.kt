package mihon.app.link

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
import mihon.core.platform.PlatformContext
import mihon.core.platform.link.LinkOpener

/** A view intent for the link, started from the caller's context; a failure, such as no app for it, shows as a toast. */
@Inject
class AndroidLinkOpener : LinkOpener {
    override fun open(context: PlatformContext, url: String, forceDefaultBrowser: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                // Force default browser so that verified extensions don't re-open Tachiyomi
                if (forceDefaultBrowser) {
                    context.defaultBrowserPackageName()?.let { setPackage(it) }
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    private fun Context.defaultBrowserPackageName(): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, "http://".toUri())
        val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                browserIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfo
            ?.activityInfo?.packageName
            ?.takeUnless { it in DeviceUtil.invalidDefaultBrowsers }
    }
}
