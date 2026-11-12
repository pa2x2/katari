package eu.kanade.presentation.more.settings.screen.touchpad

import androidx.compose.runtime.Composable
import app.katari.core.common.platform.PlatformFacility
import eu.kanade.presentation.more.settings.Preference

/**
 * The settings of the Touchpad page, which a platform offers where it reads touchpads itself
 * (`platformPresentation.touchpadSettings`).
 */
@PlatformFacility
interface TouchpadSettings {
    @Composable
    fun preferences(): List<Preference>
}
