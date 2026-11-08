package eu.kanade.presentation.more.settings.screen.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.core.common.toHomeScreenTabPreferenceValue
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.EditOff
import app.katari.presentation.kit.dialog.ConfirmDialog
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.HandlesOwnBackPress
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class HomeNavigationEditorScreen : Screen(), HandlesOwnBackPress {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val preferences = LocalAppUiGraph.current.customPreferences
        val committedDraft = remember(preferences) { preferences.readCommittedNavigationDraft() }
        val restoredDraft = remember(preferences) {
            preferences.homeScreenNavigationDraft.get().toNavigationDraftOrNull()
        }
        val initialDraft = remember(restoredDraft, committedDraft) {
            restoredDraft
                ?.takeIf { it.hasPersistedChangesFrom(committedDraft) }
                ?: committedDraft
        }
        val defaultDraft = remember { defaultHomeNavigationEditorDraft() }
        var draft by rememberSaveable(stateSaver = HomeNavigationEditorDraft.Saver) {
            mutableStateOf(initialDraft)
        }
        var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }
        val hasUnsavedChanges = draft.hasPersistedChangesFrom(committedDraft)
        val canReset = draft.hasPersistedChangesFrom(defaultDraft)

        LaunchedEffect(restoredDraft, committedDraft) {
            if (restoredDraft != null && !restoredDraft.hasPersistedChangesFrom(committedDraft)) {
                preferences.homeScreenNavigationDraft.delete()
            }
        }

        fun leave() {
            if (!hasUnsavedChanges) {
                preferences.homeScreenNavigationDraft.delete()
                navigator.pop()
            } else {
                showDiscardConfirmation = true
            }
        }

        fun updateDraft(updatedDraft: HomeNavigationEditorDraft) {
            draft = updatedDraft
            if (!updatedDraft.hasPersistedChangesFrom(committedDraft)) {
                preferences.homeScreenNavigationDraft.delete()
            } else {
                preferences.homeScreenNavigationDraft.set(updatedDraft.serialize())
            }
        }

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            onBackCompleted = ::leave,
        )

        HomeNavigationEditorContent(
            draft = draft,
            onDraftChange = ::updateDraft,
            onNavigateUp = ::leave,
            onReset = { updateDraft(defaultDraft) },
            onSave = {
                preferences.homeScreenTabOrder.set(draft.configuration.tabOrder)
                preferences.homeScreenTabs.set(draft.configuration.enabledTabs.toHomeScreenTabPreferenceValue())
                preferences.homeScreenPrimaryTabs.set(draft.configuration.primaryTabs)
                preferences.homeScreenStartupTab.set(draft.startupTab)
                preferences.homeScreenNavigationDraft.delete()
                navigator.pop()
            },
            resetEnabled = canReset,
            saveEnabled = hasUnsavedChanges,
        )

        if (showDiscardConfirmation) {
            ConfirmDialog(
                title = stringResource(MR.strings.home_navigation_discard_title),
                text = stringResource(MR.strings.home_navigation_discard_message),
                confirmLabel = stringResource(MR.strings.action_discard),
                onConfirm = {
                    preferences.homeScreenNavigationDraft.delete()
                    navigator.pop()
                },
                onDismissRequest = { showDiscardConfirmation = false },
                destructive = true,
                icon = MaterialSymbols.Rounded.EditOff,
                dismissLabel = stringResource(MR.strings.home_navigation_keep_editing),
            )
        }
    }
}
