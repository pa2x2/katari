package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import app.katari.core.common.HomeScreenTabs
import app.katari.ui.R
import eu.kanade.presentation.util.rememberAnimatedVectorPainter

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
internal actual fun homeTabPainter(tab: HomeScreenTabs, selected: Boolean): Painter {
    if (tab == HomeScreenTabs.Browse || tab == HomeScreenTabs.More) {
        // These have their own way out, played forwards rather than reversing the way in
        val (enter, exit) = if (tab == HomeScreenTabs.Browse) {
            R.drawable.animated_browse_enter to R.drawable.animated_browse_exit
        } else {
            R.drawable.animated_more_enter to R.drawable.animated_more_exit
        }
        return rememberAnimatedVectorPainter(
            enter = AnimatedImageVector.animatedVectorResource(enter),
            exit = AnimatedImageVector.animatedVectorResource(exit),
            atEnd = selected,
        )
    }
    // Each animation ends on the filled icon that marks the selected destination
    val iconResource = when (tab) {
        HomeScreenTabs.Library -> R.drawable.animated_library
        HomeScreenTabs.Updates -> R.drawable.animated_updates
        HomeScreenTabs.History -> R.drawable.animated_history
        HomeScreenTabs.Browse,
        HomeScreenTabs.More,
        HomeScreenTabs.Profiles,
        HomeScreenTabs.Translator,
        HomeScreenTabs.Statistics,
        -> error("$tab has no animated icon")
    }
    val image = AnimatedImageVector.animatedVectorResource(iconResource)
    return rememberAnimatedVectorPainter(image, atEnd = selected)
}
