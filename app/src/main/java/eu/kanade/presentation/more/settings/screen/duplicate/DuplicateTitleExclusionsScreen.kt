package eu.kanade.presentation.more.settings.screen.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DuplicateTitleExclusionsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val duplicatePreferences = remember { Injekt.get<DuplicatePreferences>() }
        val patterns by duplicatePreferences.titleExclusionPatterns.collectAsState()
        val lazyListState = rememberLazyListState()
        var dialog by remember { mutableStateOf<DuplicateTitleExclusionDialog?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.pref_duplicate_detection_title_exclusions),
                    subtitle = stringResource(MR.strings.pref_duplicate_detection_title_exclusions_screen_summary),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                    actions = {
                        Button(
                            label = stringResource(MR.strings.action_restore),
                            onClick = {
                                duplicatePreferences.titleExclusionPatterns.set(
                                    DuplicateTitleExclusions.defaultPatterns,
                                )
                            },
                            style = ButtonStyle.Text,
                            enabled = patterns != DuplicateTitleExclusions.defaultPatterns,
                        )
                    },
                )
            },
            floatingActionButton = {
                CategoryFloatingActionButton(
                    lazyListState = lazyListState,
                    onCreate = { dialog = DuplicateTitleExclusionDialog.Create },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                state = lazyListState,
                contentPadding = paddingValues + PaddingValues(
                    start = KatariTheme.spacing.contentInset,
                    top = KatariTheme.spacing.s,
                    end = KatariTheme.spacing.contentInset,
                ),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                item {
                    WarningBanner(
                        textRes = MR.strings.pref_duplicate_detection_title_exclusions_help,
                        modifier = Modifier.padding(bottom = KatariTheme.spacing.s),
                    )
                }
                if (patterns.isEmpty()) {
                    item {
                        EmptyScreen(MR.strings.pref_duplicate_detection_title_exclusions_empty)
                    }
                } else {
                    itemsIndexed(patterns) { index, pattern ->
                        DuplicateTitleExclusionItem(
                            pattern = pattern,
                            canMoveUp = index > 0,
                            canMoveDown = index < patterns.lastIndex,
                            onMoveUp = {
                                duplicatePreferences.titleExclusionPatterns.set(patterns.swap(index, index - 1))
                            },
                            onMoveDown = {
                                duplicatePreferences.titleExclusionPatterns.set(patterns.swap(index, index + 1))
                            },
                            onEdit = {
                                dialog = DuplicateTitleExclusionDialog.Edit(index, pattern)
                            },
                            onDelete = {
                                dialog = DuplicateTitleExclusionDialog.Delete(index, pattern)
                            },
                        )
                    }
                }
            }
        }

        when (val currentDialog = dialog) {
            null -> Unit
            DuplicateTitleExclusionDialog.Create -> {
                DuplicateTitleExclusionEditorDialog(
                    title = stringResource(MR.strings.pref_duplicate_detection_title_exclusions_add),
                    confirmLabel = stringResource(MR.strings.action_add),
                    initialValue = "",
                    existingPatterns = patterns,
                    onDismissRequest = { dialog = null },
                    onConfirm = {
                        duplicatePreferences.titleExclusionPatterns.set(patterns + it)
                        dialog = null
                    },
                )
            }
            is DuplicateTitleExclusionDialog.Edit -> {
                DuplicateTitleExclusionEditorDialog(
                    title = stringResource(MR.strings.pref_duplicate_detection_title_exclusions_edit),
                    confirmLabel = stringResource(MR.strings.action_ok),
                    initialValue = currentDialog.pattern,
                    existingPatterns = patterns.filterIndexed { index, _ -> index != currentDialog.index },
                    onDismissRequest = { dialog = null },
                    onConfirm = { updatedPattern ->
                        duplicatePreferences.titleExclusionPatterns.set(
                            patterns.toMutableList().apply { set(currentDialog.index, updatedPattern) },
                        )
                        dialog = null
                    },
                )
            }
            is DuplicateTitleExclusionDialog.Delete -> {
                ConfirmDialog(
                    title = stringResource(MR.strings.pref_duplicate_detection_title_exclusions_delete),
                    text = stringResource(
                        MR.strings.pref_duplicate_detection_title_exclusions_delete_confirmation,
                        currentDialog.pattern,
                    ),
                    confirmLabel = stringResource(MR.strings.action_delete),
                    onConfirm = {
                        duplicatePreferences.titleExclusionPatterns.set(
                            patterns.filterIndexed { index, _ -> index != currentDialog.index },
                        )
                        dialog = null
                    },
                    onDismissRequest = { dialog = null },
                    destructive = true,
                )
            }
        }
    }
}

private fun List<String>.swap(firstIndex: Int, secondIndex: Int): List<String> {
    return toMutableList().apply {
        val value = this[firstIndex]
        this[firstIndex] = this[secondIndex]
        this[secondIndex] = value
    }
}

private sealed class DuplicateTitleExclusionDialog {
    data object Create : DuplicateTitleExclusionDialog()
    data class Edit(val index: Int, val pattern: String) : DuplicateTitleExclusionDialog()
    data class Delete(val index: Int, val pattern: String) : DuplicateTitleExclusionDialog()
}
