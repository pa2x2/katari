package mihon.feature.migration.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.presentation.entry.entryTypePresentation
import mihon.entry.interactions.migration.EntryMigrationMode
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.feature.migration.options.getLabel
import mihon.feature.migration.options.toMigrationFlag
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.Dialog
import mihon.presentation.kit.dialog.DialogAction
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun Screen.MigrateEntryDialog(
    current: Entry,
    target: Entry,
    onClickTitle: () -> Unit,
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit = onDismissRequest,
) {
    val scope = rememberCoroutineScope()

    val viewModel = viewModel { MigrateEntryDialogViewModel() }
    LaunchedEffect(current, target) {
        viewModel.init(current, target)
    }
    val state by viewModel.state.collectAsState()

    if (state.isMigrated) return

    if (state.isLoading || state.isMigrating) {
        LoadingScreen(modifier = Modifier.background(KatariTheme.colors.scrim))
        return
    }

    val migrate: (EntryMigrationMode) -> Unit = { mode ->
        scope.launchIO {
            if (viewModel.migrateEntry(mode)) {
                withUIContext { onComplete() }
            }
        }
    }
    Dialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(MR.strings.migration_dialog_what_to_include),
        confirm = DialogAction(
            label = stringResource(MR.strings.migrate),
            onClick = { migrate(EntryMigrationMode.REPLACE) },
            enabled = state.reference != null,
        ),
        dismiss = DialogAction(
            label = stringResource(MR.strings.copy),
            onClick = { migrate(EntryMigrationMode.COPY) },
            enabled = state.reference != null,
        ),
        neutral = DialogAction(
            label = stringResource(MR.strings.action_show_manga),
            onClick = {
                onDismissRequest()
                onClickTitle()
            },
        ),
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            state.availableOptions.fastForEach { option ->
                CheckRow(
                    title = stringResource(
                        when (option) {
                            EntryMigrationOption.CHILD_STATE ->
                                current.type
                                    .entryTypePresentation()
                                    .childListTitle
                            else -> option.toMigrationFlag().getLabel()
                        },
                    ),
                    checked = option in state.selectedOptions,
                    onCheckedChange = { viewModel.toggleSelection(option) },
                )
            }
            if (state.hasFailure) {
                Text(text = stringResource(MR.strings.internal_error), tone = ContentTone.Error)
            }
        }
    }
}
