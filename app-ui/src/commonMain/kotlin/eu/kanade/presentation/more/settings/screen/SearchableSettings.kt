package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import app.katari.sync.ui.choice.SettingsSyncViewAction
import app.katari.sync.ui.choice.SettingsSyncViewQuestion
import app.katari.sync.ui.choice.settingsSyncPreferences
import app.katari.sync.ui.choice.syncMarkHint
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

    /** The rows settings search finds this screen by: all of them, unless some only repeat rows of other screens. */
    @Composable
    open fun getSearchedPreferences(): List<Preference> = getPreferences()

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
        var showsSync by rememberSaveable { mutableStateOf(false) }
        val markHint = syncMarkHint(items)
        PreferenceScaffold(
            titleRes = getTitleRes(),
            isProfileSpecific = items.isNotEmpty() && items.all { it.belongsToProfile(profileStore) },
            onBackPressed = if (handleBack != null) handleBack::invoke else null,
            actions = {
                SettingsSyncViewAction(items, showing = showsSync, onShowingChange = { showsSync = it })
                AppBarAction()
            },
            itemsProvider = {
                listOfNotNull(markHint) + if (showsSync) settingsSyncPreferences(items) else items
            },
        )
        if (showsSync) SettingsSyncViewQuestion()
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See preferenceHighlight
        var highlightKey: String? = null
    }
}
