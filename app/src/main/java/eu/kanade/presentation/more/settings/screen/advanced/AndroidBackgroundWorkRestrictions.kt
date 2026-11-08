package eu.kanade.presentation.more.settings.screen.advanced

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.util.system.powerManager

/** Android's battery optimisation, which the app asks to be exempted from. */
@Inject
class AndroidBackgroundWorkRestrictions : BackgroundWorkRestrictions {
    override val isOffered: Boolean = true

    override fun requestUnrestricted(context: PlatformContext): UnrestrictedBackgroundWorkRequest {
        val packageName: String = context.packageName
        if (context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
            return UnrestrictedBackgroundWorkRequest.AlreadyUnrestricted
        }
        return try {
            @SuppressLint("BatteryLife")
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            UnrestrictedBackgroundWorkRequest.Asked
        } catch (_: ActivityNotFoundException) {
            UnrestrictedBackgroundWorkRequest.SettingMissing
        }
    }
}
