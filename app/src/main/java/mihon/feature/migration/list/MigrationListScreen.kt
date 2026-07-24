package mihon.feature.migration.list

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.search.MigrateSearchScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryMigrationOption
import mihon.entry.interactions.EntryMigrationSubject
import mihon.feature.migration.list.components.MigrationEntryDialog
import mihon.feature.migration.list.components.MigrationExitDialog
import mihon.feature.migration.list.components.MigrationProgressDialog
import tachiyomi.i18n.MR

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
        val screenModel = rememberScreenModel {
            MigrationListScreenModel(subjects, extraSearchQuery, selectedOptions)
        }
        val state by screenModel.state.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        LaunchedEffect(matchOverride) {
            val (current, target) = matchOverride ?: return@LaunchedEffect
            screenModel.useEntryForMigration(
                current = current,
                target = target,
                onMissingChapters = {
                    context.toast(MR.strings.migrationListScreen_matchWithoutChapterToast, Toast.LENGTH_LONG)
                },
            )
            matchOverride = null
        }

        LaunchedEffect(screenModel) {
            screenModel.navigateBackEvent.collect {
                navigator.pop()
            }
        }
        LaunchedEffect(screenModel) {
            screenModel.migrationFailureEvent.collect {
                context.toast(MR.strings.internal_error)
            }
        }
        MigrationListScreenContent(
            items = state.items,
            migrationComplete = state.migrationComplete,
            finishedCount = state.finishedCount,
            onItemClick = {
                navigator.push(EntryScreen(it.id, fromSource = true))
            },
            onSearchManually = { migrationItem ->
                navigator push MigrateSearchScreen(migrationItem.subject)
            },
            onSkip = { screenModel.removeEntry(it) },
            onMigrate = { screenModel.migrateNow(entryId = it, replace = true) },
            onCopy = { screenModel.migrateNow(entryId = it, replace = false) },
            openMigrationDialog = screenModel::showMigrateDialog,
        )

        when (val dialog = state.dialog) {
            is MigrationListScreenModel.Dialog.Migrate -> {
                MigrationEntryDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    copy = dialog.copy,
                    totalCount = dialog.totalCount,
                    skippedCount = dialog.skippedCount,
                    onMigrate = {
                        if (dialog.copy) {
                            screenModel.copyEntries()
                        } else {
                            screenModel.migrateEntries()
                        }
                    },
                )
            }
            is MigrationListScreenModel.Dialog.Progress -> {
                MigrationProgressDialog(
                    progress = dialog.progress,
                    exitMigration = screenModel::cancelMigrate,
                )
            }
            MigrationListScreenModel.Dialog.Exit -> {
                MigrationExitDialog(
                    onDismissRequest = screenModel::dismissDialog,
                    exitMigration = navigator::pop,
                )
            }
            null -> Unit
        }

        BackHandler(true) {
            screenModel.showExitDialog()
        }
    }
}
