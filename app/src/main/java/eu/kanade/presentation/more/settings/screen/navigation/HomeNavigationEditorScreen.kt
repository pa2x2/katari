package eu.kanade.presentation.more.settings.screen.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.HandlesOwnBackPress
import eu.kanade.presentation.util.Screen
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.navigation.defaultHomeNavigationConfiguration
import mihon.core.common.resolveHomeScreenTab
import mihon.core.common.toHomeScreenTabPreferenceValue
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class HomeNavigationEditorScreen : Screen(), HandlesOwnBackPress {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = remember { Injekt.get<CustomPreferences>() }
        val committedDraft = remember(preferences) { preferences.readCommittedNavigationDraft() }
        val initialDraft = remember(preferences, committedDraft) {
            preferences.homeScreenNavigationDraft.get().toNavigationDraftOrNull() ?: committedDraft
        }
        var draft by rememberSaveable(stateSaver = HomeNavigationEditorDraft.Saver) {
            mutableStateOf(initialDraft)
        }
        var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }

        fun leave() {
            if (draft == committedDraft) {
                preferences.homeScreenNavigationDraft.delete()
                navigator.pop()
            } else {
                showDiscardConfirmation = true
            }
        }

        fun updateDraft(updatedDraft: HomeNavigationEditorDraft) {
            draft = updatedDraft
            if (updatedDraft == committedDraft) {
                preferences.homeScreenNavigationDraft.delete()
            } else {
                preferences.homeScreenNavigationDraft.set(updatedDraft.serialize())
            }
        }

        BackHandler(onBack = ::leave)

        HomeNavigationEditorContent(
            draft = draft,
            onDraftChange = ::updateDraft,
            onNavigateUp = ::leave,
            onReset = {
                val defaults = defaultHomeNavigationConfiguration()
                updateDraft(
                    HomeNavigationEditorDraft(
                        configuration = defaults,
                        startupTab = resolveHomeScreenTab(
                            requestedTab = HomeScreenTabs.Library,
                            enabledTabs = defaults.enabledTabs.filterNot { it == HomeScreenTabs.Profiles },
                            tabOrder = defaults.tabOrder,
                        ),
                        previewTab = HomeScreenTabs.Library,
                    ),
                )
            },
            onSave = {
                preferences.homeScreenTabOrder.set(draft.configuration.tabOrder)
                preferences.homeScreenTabs.set(draft.configuration.enabledTabs.toHomeScreenTabPreferenceValue())
                preferences.homeScreenPrimaryTabs.set(draft.configuration.primaryTabs)
                preferences.homeScreenStartupTab.set(draft.startupTab)
                preferences.homeScreenNavigationDraft.delete()
                navigator.pop()
            },
        )

        if (showDiscardConfirmation) {
            AlertDialog(
                onDismissRequest = { showDiscardConfirmation = false },
                title = { Text(stringResource(MR.strings.home_navigation_discard_title)) },
                text = { Text(stringResource(MR.strings.home_navigation_discard_message)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            preferences.homeScreenNavigationDraft.delete()
                            navigator.pop()
                        },
                    ) {
                        Text(stringResource(MR.strings.action_discard))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardConfirmation = false }) {
                        Text(stringResource(MR.strings.home_navigation_keep_editing))
                    }
                },
            )
        }
    }
}
