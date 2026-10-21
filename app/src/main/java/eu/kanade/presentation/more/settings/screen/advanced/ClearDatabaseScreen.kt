package eu.kanade.presentation.more.settings.screen.advanced

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.util.fastMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.browse.components.SourceIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import mihon.app.di.AppUiScope
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import mihon.entry.interactions.lifecycle.removal.EntryDestructiveRemovalResult
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.control.Checkbox
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchUI
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.interactor.GetSourcesWithNonLibraryEntries
import tachiyomi.domain.source.model.Source
import tachiyomi.domain.source.model.SourceWithCount
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

class ClearDatabaseScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = metroViewModel<ClearDatabaseViewModel>()
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        when (val s = state) {
            is ClearDatabaseViewModel.State.Loading -> LoadingScreen()
            is ClearDatabaseViewModel.State.Ready -> {
                if (s.showConfirmation) {
                    ClearDatabaseConfirmDialog(
                        onConfirm = { keepReadManga ->
                            scope.launchUI {
                                model.removeMangaBySourceId(keepReadManga)
                                model.clearSelection()
                                model.hideConfirmation()
                                context.toast(MR.strings.clear_database_completed)
                            }
                        },
                        onDismissRequest = model::hideConfirmation,
                    )
                }

                Scaffold(
                    topBar = { scrollBehavior ->
                        AppBar(
                            title = stringResource(MR.strings.pref_clear_database),
                            navigateUp = navigator::pop,
                            actions = {
                                if (s.items.isNotEmpty()) {
                                    AppBarActions(
                                        actions = listOf(
                                            AppBar.Action(
                                                title = stringResource(MR.strings.action_select_all),
                                                icon = MaterialSymbols.Rounded.SelectAll,
                                                onClick = model::selectAll,
                                            ),
                                            AppBar.Action(
                                                title = stringResource(MR.strings.action_select_inverse),
                                                icon = MaterialSymbols.Rounded.Deselect,
                                                onClick = model::invertSelection,
                                            ),
                                        ),
                                    )
                                }
                            },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                    bottomBar = {
                        if (s.items.isNotEmpty()) {
                            BottomActionBar {
                                Button(
                                    label = stringResource(MR.strings.action_delete),
                                    onClick = model::showConfirmation,
                                    modifier = Modifier.weight(1f),
                                    enabled = s.selection.isNotEmpty(),
                                )
                            }
                        }
                    },
                ) { contentPadding ->
                    if (s.items.isEmpty()) {
                        EmptyScreen(
                            message = stringResource(MR.strings.database_clean),
                            modifier = Modifier.padding(contentPadding),
                        )
                    } else {
                        ScrollbarLazyColumn(contentPadding = contentPadding) {
                            rowGroupItems(
                                items = s.items,
                                key = { it.id },
                                selected = { it.id in s.selection },
                            ) { sourceWithCount ->
                                ClearDatabaseItem(
                                    source = sourceWithCount.source,
                                    count = sourceWithCount.count,
                                    isSelected = s.selection.contains(sourceWithCount.id),
                                    onClickSelect = { model.toggleSelection(sourceWithCount.source) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ClearDatabaseItem(
        source: Source,
        count: Long,
        isSelected: Boolean,
        onClickSelect: () -> Unit,
    ) {
        SettingsRow(
            title = source.visualName,
            subtitle = stringResource(MR.strings.clear_database_source_item_count, count),
            leading = { SourceIcon(source = source) },
            trailing = { Checkbox(checked = isSelected, onCheckedChange = null) },
            role = Role.Checkbox,
            onClick = onClickSelect,
        )
    }
}

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
internal class ClearDatabaseViewModel(
    private val getSourcesWithNonLibraryEntries: GetSourcesWithNonLibraryEntries,
    private val entryRepository: EntryRepository,
    private val destructiveRemoval: EntryDestructiveRemovalFeature,
) : ViewModel() {

    val state: StateFlow<ClearDatabaseViewModel.State>
        field = MutableStateFlow<ClearDatabaseViewModel.State>(State.Loading)

    init {
        viewModelScope.launchIO {
            getSourcesWithNonLibraryEntries.subscribe()
                .collectLatest { list ->
                    state.update { old ->
                        val items = list.sortedBy { it.name }
                        when (old) {
                            State.Loading -> State.Ready(items)
                            is State.Ready -> old.copy(items = items)
                        }
                    }
                }
        }
    }

    // Removal deletes each entry's cover files on the calling thread
    suspend fun removeMangaBySourceId(keepReadManga: Boolean) = withContext(NonCancellable + Dispatchers.IO) {
        val state = state.value as? State.Ready ?: return@withContext
        val entries = entryRepository.getNonLibraryEntriesBySources(
            sourceIds = state.selection,
            keepReadEntries = keepReadManga,
        )
        when (val result = destructiveRemoval.remove(entries)) {
            is EntryDestructiveRemovalResult.Failed -> throw result.cause
            EntryDestructiveRemovalResult.NoChange,
            is EntryDestructiveRemovalResult.Removed,
            -> Unit
        }
    }

    fun toggleSelection(source: Source) = state.update { state ->
        if (state !is State.Ready) return@update state
        val mutableList = state.selection.toMutableList()
        if (mutableList.contains(source.id)) {
            mutableList.remove(source.id)
        } else {
            mutableList.add(source.id)
        }
        state.copy(selection = mutableList)
    }

    fun clearSelection() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = emptyList())
    }

    fun selectAll() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(selection = state.items.fastMap { it.id })
    }

    fun invertSelection() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(
            selection = state.items
                .fastMap { it.id }
                .filterNot { it in state.selection },
        )
    }

    fun showConfirmation() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = true)
    }

    fun hideConfirmation() = state.update { state ->
        if (state !is State.Ready) return@update state
        state.copy(showConfirmation = false)
    }

    sealed interface State {
        @Immutable
        data object Loading : State

        @Immutable
        data class Ready(
            val items: List<SourceWithCount>,
            val selection: List<Long> = emptyList(),
            val showConfirmation: Boolean = false,
        ) : State
    }
}
