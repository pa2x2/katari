package eu.kanade.presentation.more.settings.screen.animeplayer

import androidx.compose.runtime.Composable
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen

/** The default subtitle look's page, which the anime player provides, as it owns how subtitles look. */
class DefaultSubtitleLookScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        LocalAppUiGraph.current.defaultSubtitleLookPage.Content(navigateUp = navigator::pop)
    }
}
