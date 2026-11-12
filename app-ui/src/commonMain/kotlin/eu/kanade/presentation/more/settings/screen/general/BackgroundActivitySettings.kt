package eu.kanade.presentation.more.settings.screen.general

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility
import eu.kanade.presentation.more.settings.Preference

/**
 * The settings that keep the app working while the person isn't looking at it, which each platform's system decides
 * differently: on Android the limits of battery optimisation. The General page hides its background activity group
 * while there are none.
 */
@PlatformFacility
interface BackgroundActivitySettings {
    @Composable
    fun preferences(): List<Preference.PreferenceItem<out Any, out Any>>
}
