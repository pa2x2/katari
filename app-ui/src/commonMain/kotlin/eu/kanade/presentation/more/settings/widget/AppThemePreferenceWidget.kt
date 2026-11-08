package eu.kanade.presentation.more.settings.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.list.ThemeChoiceRow
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.domain.ui.model.AppTheme

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
