package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.katari.ui.di.AppUiGraph
import app.katari.ui.di.LocalAppUiGraph
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.belongsToProfile
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen

abstract class SearchableSettings : Screen() {

    @Composable
    @ReadOnlyComposable
    abstract fun getTitleRes(): StringResource

    @Composable
    abstract fun getPreferences(): List<Preference>

    /**
     * False where the platform offers nothing this screen sets, such as speech settings without speech; the settings
     * list and the settings search then leave the screen out.
     */
    open fun isOffered(graph: AppUiGraph): Boolean = true

    @Composable
    open fun RowScope.AppBarAction() {
    }

    @Composable
    override fun Content() {
        val handleBack = LocalBackPress.current
        val items = getPreferences()
        val profileStore = LocalAppUiGraph.current.profileStore
        PreferenceScaffold(
            titleRes = getTitleRes(),
            isProfileSpecific = items.isNotEmpty() && items.all { it.belongsToProfile(profileStore) },
            onBackPressed = if (handleBack != null) handleBack::invoke else null,
            actions = { AppBarAction() },
            itemsProvider = { items },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See preferenceHighlight
        var highlightKey: String? = null
    }
}
