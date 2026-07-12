package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import cafe.adriel.voyager.core.screen.Screen
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScaffold
import eu.kanade.presentation.more.settings.isFullyProfileSpecific
import eu.kanade.presentation.util.LocalBackPress

interface SearchableSettings : Screen {

    @Composable
    @ReadOnlyComposable
    fun getTitleRes(): StringResource

    @Composable
    fun getPreferences(): List<Preference>

    @Composable
    fun RowScope.AppBarAction() {
    }

    @Composable
    override fun Content() {
        val handleBack = LocalBackPress.current
        val items = getPreferences()
        val isFullyProfileSpecific = items.isNotEmpty() && items.all { preference ->
            when (preference) {
                is Preference.PreferenceGroup -> preference.isFullyProfileSpecific()
                is Preference.PreferenceItem<*, *> -> preference.isProfileSpecific
            }
        }
        PreferenceScaffold(
            titleRes = getTitleRes(),
            isProfileSpecific = isFullyProfileSpecific,
            onBackPressed = if (handleBack != null) handleBack::invoke else null,
            actions = { AppBarAction() },
            itemsProvider = { items },
        )
    }

    companion object {
        // HACK: for the background blipping thingy.
        // The title of the target PreferenceItem
        // Set before showing the destination screen and reset after
        // See BasePreferenceWidget.highlightBackground
        var highlightKey: String? = null
    }
}
