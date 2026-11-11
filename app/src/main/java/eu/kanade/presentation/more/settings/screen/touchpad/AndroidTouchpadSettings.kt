package eu.kanade.presentation.more.settings.screen.touchpad

import androidx.compose.runtime.Composable
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.Preference

/**
 * Android hands the app a touchpad's swipes as touches, already in the direction its own settings choose, so it offers
 * no Touchpad page and this has nothing to show.
 */
@Inject
class AndroidTouchpadSettings : TouchpadSettings {
    @Composable
    override fun preferences(): List<Preference> = emptyList()
}
