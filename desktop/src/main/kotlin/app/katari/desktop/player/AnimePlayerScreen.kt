package app.katari.desktop.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.MediaBackdrop
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.home.navigation.FullWindowScreen

/**
 * The player's place in the window's navigation while it fills the main window: the dock steps aside, back leaves
 * it, and screens it opens go over it. The player itself is drawn over it by [Host], so it can leave the navigation
 * for picture-in-picture and come back without being composed anew; this shows only the media colour under it.
 */
internal class AnimePlayerScreen : Screen(), FullWindowScreen {
    @Composable
    override fun Content() {
        MediaBackdrop(Modifier.fillMaxSize())
    }
}
