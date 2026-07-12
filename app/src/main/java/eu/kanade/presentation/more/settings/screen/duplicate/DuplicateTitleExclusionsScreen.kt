package eu.kanade.presentation.more.settings.screen.duplicate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.components.CategoryFloatingActionButton
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.delay
import tachiyomi.domain.library.service.DuplicatePreferences
import tachiyomi.domain.library.service.DuplicateTitleExclusions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.collectAsState
import tachiyomi.presentation.core.util.plus
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

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
                        TextButton(
                            enabled = patterns != DuplicateTitleExclusions.defaultPatterns,
                            onClick = {
                                duplicatePreferences.titleExclusionPatterns.set(
                                    DuplicateTitleExclusions.defaultPatterns,
                                )
                            },
                        ) {
                            Text(stringResource(MR.strings.action_restore))
                        }
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
                contentPadding = paddingValues + topSmallPaddingValues +
                    PaddingValues(horizontal = MaterialTheme.padding.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                item {
                    WarningBanner(
                        textRes = MR.strings.pref_duplicate_detection_title_exclusions_help,
                        modifier = Modifier.padding(bottom = MaterialTheme.padding.small),
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
                AlertDialog(
                    onDismissRequest = { dialog = null },
                    title = { Text(stringResource(MR.strings.pref_duplicate_detection_title_exclusions_delete)) },
                    text = {
                        Text(
                            stringResource(
                                MR.strings.pref_duplicate_detection_title_exclusions_delete_confirmation,
                                currentDialog.pattern,
                            ),
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                duplicatePreferences.titleExclusionPatterns.set(
                                    patterns.filterIndexed { index, _ -> index != currentDialog.index },
                                )
                                dialog = null
                            },
                        ) {
                            Text(stringResource(MR.strings.action_delete))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialog = null }) {
                            Text(stringResource(MR.strings.action_cancel))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun DuplicateTitleExclusionItem(
    pattern: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pattern,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(enabled = canMoveUp, onClick = onMoveUp) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(MR.strings.action_move_to_top),
                )
            }
            IconButton(enabled = canMoveDown, onClick = onMoveDown) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(MR.strings.action_move_to_bottom),
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(MR.strings.action_edit),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(MR.strings.action_delete),
                )
            }
        }
    }
}

@Composable
private fun DuplicateTitleExclusionEditorDialog(
    title: String,
    confirmLabel: String,
    initialValue: String,
    existingPatterns: List<String>,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var pattern by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val validation = remember(pattern, existingPatterns) {
        validatePattern(pattern, existingPatterns)
    }
    val normalizedPattern = remember(pattern) { DuplicateTitleExclusions.normalizePattern(pattern) }
    val normalizedInitialValue = remember(initialValue) { DuplicateTitleExclusions.normalizePattern(initialValue) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(stringResource(MR.strings.pref_duplicate_detection_title_exclusions_pattern)) },
                supportingText = {
                    Text(
                        if (validation.message != null) {
                            stringResource(validation.message)
                        } else {
                            stringResource(MR.strings.pref_duplicate_detection_title_exclusions_pattern_help)
                        },
                    )
                },
                isError = !validation.isValid,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                enabled = validation.isValid && normalizedPattern != normalizedInitialValue,
                onClick = {
                    onConfirm(normalizedPattern)
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
    )

    LaunchedEffect(focusRequester) {
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

private fun validatePattern(pattern: String, existingPatterns: List<String>): PatternValidation {
    val normalized = DuplicateTitleExclusions.normalizePattern(pattern)
    return when {
        normalized.isBlank() -> PatternValidation(
            false,
            MR.strings.pref_duplicate_detection_title_exclusions_error_blank,
        )
        DuplicateTitleExclusions.isCatchAllPattern(normalized) ->
            PatternValidation(false, MR.strings.pref_duplicate_detection_title_exclusions_error_catch_all)
        existingPatterns.any { it.equals(normalized, ignoreCase = true) } ->
            PatternValidation(false, MR.strings.pref_duplicate_detection_title_exclusions_error_exists)
        else -> PatternValidation(true, null)
    }
}

private fun List<String>.swap(firstIndex: Int, secondIndex: Int): List<String> {
    return toMutableList().apply {
        val value = this[firstIndex]
        this[firstIndex] = this[secondIndex]
        this[secondIndex] = value
    }
}

private data class PatternValidation(
    val isValid: Boolean,
    val message: StringResource?,
)

private sealed class DuplicateTitleExclusionDialog {
    data object Create : DuplicateTitleExclusionDialog()
    data class Edit(val index: Int, val pattern: String) : DuplicateTitleExclusionDialog()
    data class Delete(val index: Int, val pattern: String) : DuplicateTitleExclusionDialog()
}
