package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import mihon.presentation.kit.list.RowGroup
import tachiyomi.presentation.core.util.collectAsState

internal class ThemeStep(
    private val uiPreferences: UiPreferences,
) : OnboardingStep {

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val themeModePref = uiPreferences.themeMode
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled
        val amoled by amoledPref.collectAsState()

        RowGroup(
            rows = listOf {
                Column {
                    AppThemeModePreferenceWidget(
                        value = themeMode,
                        onItemClick = {
                            themeModePref.set(it)
                            setAppCompatDelegateThemeMode(it)
                        },
                    )

                    AppThemePreferenceWidget(
                        value = appTheme,
                        amoled = amoled,
                        onItemClick = { appThemePref.set(it) },
                    )
                }
            },
        )
    }
}
