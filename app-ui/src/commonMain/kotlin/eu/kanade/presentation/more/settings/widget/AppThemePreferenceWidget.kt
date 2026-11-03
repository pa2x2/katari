package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.model.AppTheme
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.presentation.kit.list.ThemeChoiceRow

@Composable
fun AppThemePreferenceWidget(
    value: AppTheme,
    amoled: Boolean,
    onItemClick: (AppTheme) -> Unit,
) {
    val context = currentPlatformContext()
    val themeHost = LocalAppUiGraph.current.appThemeHost
    val appThemes = remember(themeHost) {
        AppTheme.entries
            .filterNot { it.titleRes == null || (it == AppTheme.MONET && !themeHost.isDynamicColorAvailable) }
    }
    ThemeChoiceRow(
        themes = appThemes,
        selected = value,
        amoled = amoled,
        onSelect = { appTheme ->
            onItemClick(appTheme)
            themeHost.rebuildForTheme(context)
        },
    )
}
