package eu.kanade.presentation.more.settings.screen.general

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import app.katari.core.platform.PlatformContext
import eu.kanade.tachiyomi.util.system.powerManager

/** Asks Android to exempt the app from battery optimisation, which otherwise stops its work in the background. */
internal object UnrestrictedBackground {
    fun request(context: PlatformContext): UnrestrictedRequest {
        val packageName: String = context.packageName
        if (context.powerManager.isIgnoringBatteryOptimizations(packageName)) {
            return UnrestrictedRequest.AlreadyUnrestricted
        }
        return try {
            @SuppressLint("BatteryLife")
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = "package:$packageName".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            UnrestrictedRequest.Asked
        } catch (_: ActivityNotFoundException) {
            UnrestrictedRequest.SettingMissing
        }
    }
}

internal enum class UnrestrictedRequest {
    /** The system's prompt was opened. */
    Asked,

    /** The app already works unrestricted, so nothing was asked. */
    AlreadyUnrestricted,

    /** The device has no screen to ask on. */
    SettingMissing,
}
