package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.BaseEntryListItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.domain.entry.model.Entry
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.util.selectedBackground
import tachiyomi.presentation.core.util.shouldExpandFAB

data class MigrateEntriesScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MigrateEntriesScreenModel(sourceId) }

        val state by screenModel.state.collectAsState()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        BackHandler(enabled = state.selectionMode) {
            screenModel.clearSelection()
        }

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = state.source!!.name,
                    navigateUp = {
                        if (state.selectionMode) {
                            screenModel.clearSelection()
                        } else {
                            navigator.pop()
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                    icon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    },
                    onClick = {
                        val selection = state.selection
                        screenModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.selectionMode,
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
        ) { contentPadding ->
            if (state.isEmpty) {
                EmptyScreen(
                    stringRes = MR.strings.empty_screen,
                    modifier = Modifier.padding(contentPadding),
                )
                return@Scaffold
            }

            MigrateEntriesContent(
                lazyListState = lazyListState,
                contentPadding = contentPadding,
                state = state,
                onClickItem = screenModel::toggleSelection,
                onClickCover = { navigator.push(EntryScreen(it.id)) },
            )
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                when (event) {
                    MigrationEntriesEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
                    }
                }
            }
        }
    }

    @Composable
    private fun MigrateEntriesContent(
        lazyListState: LazyListState,
        contentPadding: PaddingValues,
        state: MigrateEntriesScreenModel.State,
        onClickItem: (Entry) -> Unit,
        onClickCover: (Entry) -> Unit,
    ) {
        FastScrollLazyColumn(
            state = lazyListState,
            contentPadding = contentPadding,
        ) {
            items(state.entries) { entry ->
                MigrateEntryItem(
                    entry = entry,
                    isSelected = entry.id in state.selection,
                    onClickItem = onClickItem,
                    onClickCover = onClickCover,
                )
            }
        }
    }

    @Composable
    private fun MigrateEntryItem(
        entry: Entry,
        isSelected: Boolean,
        onClickItem: (Entry) -> Unit,
        onClickCover: (Entry) -> Unit,
        modifier: Modifier = Modifier,
    ) {
        BaseEntryListItem(
            modifier = modifier.selectedBackground(isSelected),
            entry = entry,
            onClickItem = { onClickItem(entry) },
            onClickCover = { onClickCover(entry) },
        )
    }
}
