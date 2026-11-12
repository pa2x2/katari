package eu.kanade.presentation.more.settings.screen.general

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import app.katari.core.platform.PlatformContext
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.rememberScreenMessages
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.util.system.powerManager
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Android's battery optimisation, which can stop library updates and downloads, and which the app asks to be exempted
 * from, together with the restrictions some manufacturers add on top of it.
 */
@Inject
class AndroidBackgroundActivitySettings : BackgroundActivitySettings {
    @Composable
    override fun preferences(): List<Preference.PreferenceItem<out Any, out Any>> {
        val context = currentPlatformContext()
        val messages = rememberScreenMessages()
        val uriHandler = LocalUriHandler.current
        return listOf(
            Preference.PreferenceItem.TextPreference(
                title = stringResource(PlatformStrings.pref_disable_battery_optimization),
                subtitle = stringResource(PlatformStrings.pref_disable_battery_optimization_summary),
                onClick = {
                    when (requestUnrestricted(context)) {
                        UnrestrictedRequest.Asked -> Unit
                        UnrestrictedRequest.SettingMissing ->
                            messages.show(PlatformStrings.battery_optimization_setting_activity_not_found)
                        UnrestrictedRequest.AlreadyUnrestricted ->
                            messages.show(PlatformStrings.battery_optimization_disabled)
                    }
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = "Don't kill my app!",
                subtitle = stringResource(PlatformStrings.about_dont_kill_my_app),
                onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
            ),
        )
    }

    private fun requestUnrestricted(context: PlatformContext): UnrestrictedRequest {
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

    private enum class UnrestrictedRequest {
        /** The system's prompt was opened. */
        Asked,

        /** The app already works unrestricted, so nothing was asked. */
        AlreadyUnrestricted,

        /** The device has no screen to ask on. */
        SettingMissing,
    }
}
