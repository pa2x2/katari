package eu.kanade.presentation.more.settings.screen.libraryupdates

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.visualName
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.util.Screen
import mihon.feature.library.update.planning.LibraryUpdateSkipRule
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

/** One category's switch, and the skip rules and interval it can use instead of the library's. */
class CategoryUpdateRulesScreen(private val categoryId: Long) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { CategoryUpdateRulesScreenModel(categoryId) }
        val state by screenModel.state.collectAsState()
        val overrides = state.rules.overrides

        Scaffold(
            topBar = {
                AppBar(
                    titleContent = {
                        AppBarTitle(
                            title = state.category?.visualName.orEmpty(),
                            titleSuffix = { ProfileSpecificChip() },
                        )
                    },
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            PreferenceScreen(
                contentPadding = contentPadding,
                items = listOfNotNull(
                    switchItem(
                        title = stringResource(MR.strings.library_updates_check_automatically),
                        subtitle = stringResource(MR.strings.library_updates_category_off_summary)
                            .takeIf { !state.rules.autoUpdate },
                        checked = state.rules.autoUpdate,
                        onClick = { screenModel.setAutoUpdate(!state.rules.autoUpdate) },
                    ),
                    switchItem(
                        title = stringResource(MR.strings.library_updates_use_library_rules),
                        checked = overrides == null,
                        enabled = state.rules.autoUpdate,
                        onClick = { screenModel.setUseLibraryRules(overrides != null) },
                    ),
                    overrides?.let {
                        Preference.PreferenceGroup(
                            title = stringResource(MR.strings.library_updates_skip_when),
                            enabled = state.rules.autoUpdate,
                            preferenceItems = LibraryUpdateSkipRule.entries.map { rule ->
                                val on = rule.isOn(overrides.skipRules)
                                switchItem(
                                    title = stringResource(rule.titleRes),
                                    subtitle = stringResource(
                                        if (rule.isOn(state.librarySkipRules)) {
                                            MR.strings.library_updates_library_on
                                        } else {
                                            MR.strings.library_updates_library_off
                                        },
                                    ),
                                    checked = on,
                                    enabled = state.rules.autoUpdate,
                                    onClick = { screenModel.setSkipRule(rule, !on) },
                                )
                            },
                        )
                    },
                    overrides?.let {
                        val libraryEntry = stringResource(
                            MR.strings.library_updates_library_interval,
                            libraryUpdateIntervalLabel(state.libraryIntervalHours),
                        )
                        Preference.PreferenceGroup(
                            title = stringResource(MR.strings.library_updates_schedule),
                            enabled = state.rules.autoUpdate,
                            preferenceItems = listOf(
                                Preference.PreferenceItem.BasicListPreference(
                                    value = overrides.intervalHours?.toString() ?: LIBRARY_INTERVAL,
                                    entries = mapOf(LIBRARY_INTERVAL to libraryEntry) +
                                        libraryUpdateIntervals().mapKeys { (hours, _) -> hours.toString() },
                                    title = stringResource(MR.strings.library_updates_check_every),
                                    enabled = state.rules.autoUpdate,
                                    isProfileSpecific = true,
                                    onValueChanged = { screenModel.setIntervalHours(it.toIntOrNull()) },
                                ),
                            ),
                        )
                    },
                ),
            )
        }
    }

    private fun switchItem(
        title: String,
        checked: Boolean,
        onClick: () -> Unit,
        subtitle: String? = null,
        enabled: Boolean = true,
    ) = Preference.PreferenceItem.TextPreference(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        isProfileSpecific = true,
        widget = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        onClick = onClick.takeIf { enabled },
    )

    private companion object {
        const val LIBRARY_INTERVAL = "library"
    }
}
