package eu.kanade.presentation.more.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.screen.appearance.AppThemeHost
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Palette
import mihon.presentation.kit.feedback.GuidePageHeading
import mihon.presentation.kit.list.RowGroup
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

internal class ThemeStep(
    private val uiPreferences: UiPreferences,
    private val themeHost: AppThemeHost,
) : OnboardingStep {

    @Composable
    override fun heading() = GuidePageHeading(
        icon = MaterialSymbols.Rounded.Palette,
        heading = stringResource(MR.strings.onboarding_appearance_heading),
        subtitle = stringResource(MR.strings.onboarding_appearance_subtitle, stringResource(MR.strings.app_name)),
    )

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
                            themeHost.applyThemeMode(it)
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
