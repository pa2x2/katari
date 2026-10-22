package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.browse.MigrateSearchScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchViewModel
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import kotlinx.coroutines.launch
import mihon.entry.interactions.migration.EntryMigrationSubject
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*

class MigrateSearchScreen(private val subject: EntryMigrationSubject) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val viewModel = assistedMetroViewModel<MigrateSearchViewModel, MigrateSearchViewModel.Factory> {
            create(subject = subject)
        }
        val state by viewModel.state.collectAsState()

        MigrateSearchScreen(
            state = state,
            fromSourceId = state.from?.source,
            navigateUp = navigator::pop,
            onChangeSearchQuery = viewModel::updateSearchQuery,
            onSearch = { viewModel.search() },
            getEntryState = { viewModel.getEntryState(it) },
            onChangeSearchFilter = viewModel::setSourceFilter,
            onToggleResults = viewModel::toggleFilterResults,
            onClickSource = { navigator.push(MigrateSourceSearchScreen(state.from!!, it.id, state.searchQuery)) },
            onClickItem = { target ->
                scope.launch {
                    if (!viewModel.acceptsTarget(target)) {
                        messages.show(MR.strings.internal_error)
                        return@launch
                    }
                    val migrateListScreen = navigator.items
                        .filterIsInstance<MigrationListScreen>()
                        .lastOrNull()

                    if (migrateListScreen == null) {
                        viewModel.setMigrateDialog(subject.entryId, target)
                    } else {
                        migrateListScreen.addMatchOverride(current = subject.entryId, target = target.id)
                        navigator.popUntil { screen -> screen is MigrationListScreen }
                    }
                }
            },
            onLongClickItem = {
                navigator.push(EntryScreen(it.id, fromSource = true))
            },
        )

        when (val dialog = state.dialog) {
            is SearchViewModel.Dialog.Migrate -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.current] so we show [dialog.target].
                    onClickTitle = {
                        navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                    },
                    onDismissRequest = { viewModel.clearDialog() },
                    onComplete = {
                        if (navigator.lastItem is EntryScreen) {
                            val lastItem = navigator.lastItem
                            navigator.popUntil { navigator.items.contains(lastItem) }
                            navigator.push(EntryScreen(dialog.target.id))
                        } else {
                            navigator.replace(EntryScreen(dialog.target.id))
                        }
                    },
                )
            }
            else -> {}
        }
    }
}
