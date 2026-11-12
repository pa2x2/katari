package eu.kanade.presentation.more.settings.screen.general

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.rememberScreenMessages
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference
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
                    when (UnrestrictedBackground.request(context)) {
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
}
