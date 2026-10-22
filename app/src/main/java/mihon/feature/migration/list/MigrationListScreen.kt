package mihon.feature.migration.list

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.entry.interactions.migration.EntryMigrationOption
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.list.components.MigrationEntryDialog
import mihon.feature.migration.list.components.MigrationExitDialog
import mihon.feature.migration.list.components.MigrationProgressDialog
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*

class MigrationListScreen(
    private val subjects: Collection<EntryMigrationSubject>,
    private val extraSearchQuery: String?,
    private val selectedOptions: Set<EntryMigrationOption>,
) : Screen() {

    private var matchOverride: Pair<Long, Long>? = null

    fun addMatchOverride(current: Long, target: Long) {
        matchOverride = current to target
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<MigrationListViewModel, MigrationListViewModel.Factory> {
            create(subjects, extraSearchQuery, selectedOptions)
        }
        val state by viewModel.state.collectAsState()
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()

        LaunchedEffect(matchOverride) {
            val (current, target) = matchOverride ?: return@LaunchedEffect
            viewModel.useEntryForMigration(
                current = current,
                target = target,
                onMissingChapters = {
                    messages.show(MR.strings.migrationListScreen_matchWithoutChapterToast, long = true)
                },
            )
            matchOverride = null
        }

        LaunchedEffect(viewModel) {
            viewModel.navigateBackEvent.collect {
                navigator.pop()
            }
        }
        LaunchedEffect(viewModel) {
            viewModel.migrationFailureEvent.collect {
                messages.show(MR.strings.internal_error)
            }
        }
        MigrationListScreenContent(
            items = state.items,
            migrationComplete = state.migrationComplete,
            finishedCount = state.finishedCount,
            onItemClick = { entry, mergeContext ->
                navigator.push(
                    EntryScreen(
                        entry.id,
                        fromSource = true,
                        bypassMerge = mergeContext?.isRoot == false,
                    ),
                )
            },
            onSearchManually = { migrationItem ->
                navigator push MigrateSearchScreen(migrationItem.subject)
            },
            onSkip = { viewModel.removeEntry(it) },
            onMigrate = { viewModel.showMigrateDialog(entryId = it, copy = false) },
            onCopy = { viewModel.showMigrateDialog(entryId = it, copy = true) },
            openMigrationDialog = viewModel::showMigrateDialog,
        )

        when (val dialog = state.dialog) {
            is MigrationListViewModel.Dialog.Migrate -> {
                MigrationEntryDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    copy = dialog.copy,
                    totalCount = dialog.totalCount,
                    skippedCount = dialog.skippedCount,
                    mergeImpact = dialog.mergeImpact,
                    onMigrate = { viewModel.confirmMigration(dialog) },
                )
            }
            is MigrationListViewModel.Dialog.Progress -> {
                MigrationProgressDialog(
                    progress = dialog.progress,
                    exitMigration = viewModel::cancelMigrate,
                )
            }
            MigrationListViewModel.Dialog.Exit -> {
                MigrationExitDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    exitMigration = navigator::pop,
                )
            }
            null -> Unit
        }

        BackHandler(true) {
            viewModel.showExitDialog()
        }
    }
}
