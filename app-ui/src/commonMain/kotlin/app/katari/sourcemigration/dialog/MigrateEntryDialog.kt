package app.katari.sourcemigration.dialog

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
import app.katari.entry.interactions.migration.EntryMigrationMode
import app.katari.entry.interactions.migration.EntryMigrationOption
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.dialog.Dialog
import app.katari.presentation.kit.dialog.DialogAction
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.surface.background
import app.katari.presentation.kit.theme.ContentTone
import app.katari.sourcemigration.options.getLabel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.entry.entryTypePresentation
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun MigrateEntryDialog(
    current: Entry,
    target: Entry,
    onClickTitle: () -> Unit,
    onDismissRequest: () -> Unit,
    onComplete: () -> Unit = onDismissRequest,
) {
    val scope = rememberCoroutineScope()

    val viewModel = metroViewModel<MigrateEntryDialogViewModel>()
    LaunchedEffect(current, target) {
        viewModel.init(current, target)
    }
    val state by viewModel.state.collectAsState()

    if (state.isMigrated) return

    if (state.isLoading || state.isMigrating) {
        LoadingScreen(modifier = Modifier.background(SurfaceContainer.Scrim))
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
                            else -> option.getLabel()
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
