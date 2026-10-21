package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import mihon.app.di.AppUiScope
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.feature.profiles.core.ProfileManager
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.presentation.core.util.plus

class AnimePlayerLauncherScreen : Screen() {

    companion object {
        const val TITLE = "Anime player launcher"
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = metroViewModel<Model>()
        val entryOpenFeature = viewModel.entryOpenFeature
        val state by viewModel.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = TITLE,
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            when (val current = state) {
                is State.Loading -> LoadingScreen(Modifier.padding(contentPadding))

                is State.Empty -> {
                    EmptyScreen(
                        message = "No stored anime episodes found in the active ANIME profile.",
                        modifier = Modifier.padding(contentPadding),
                    )
                }
                is State.Ready -> {
                    LazyColumn(contentPadding = contentPadding) {
                        rowGroupItems(current.items, key = { item -> item.chapter.id }) { item ->
                            TextPreferenceWidget(
                                title = item.animeTitle,
                                subtitle = item.episodeName,
                                onPreferenceClick = {
                                    entryOpenFeature.open(
                                        context = context,
                                        entry = item.entry,
                                        chapter = item.chapter,
                                    )
                                    Unit
                                }.takeIf { entryOpenFeature.isApplicable(item.entry.type) },
                            )
                        }
                    }
                }
            }
        }
    }

    @Inject
    @ViewModelKey
    @ContributesIntoMap(AppUiScope::class)
    internal class Model(
        val entryOpenFeature: EntryOpenFeature,
        private val profileManager: ProfileManager,
        private val entryRepository: EntryRepository,
        private val entryChapterRepository: EntryChapterRepository,
    ) : ViewModel() {

        val state = profileManager.activeProfile
            .flatMapLatest { activeProfile ->
                flow {
                    emit(
                        when {
                            activeProfile == null -> State.Loading
                            else -> {
                                val items = entryRepository.getAllEntriesByProfile(activeProfile.id)
                                    .filter { it.type == EntryType.ANIME }
                                    .sortedBy { it.displayTitle.lowercase() }
                                    .flatMap { entry ->
                                        entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
                                            .sortedBy { chapter -> chapter.chapterNumber }
                                            .map { chapter ->
                                                AnimeLauncherItem(
                                                    entry = entry,
                                                    chapter = chapter,
                                                    animeTitle = entry.displayTitle,
                                                    episodeName = chapter.name.ifBlank { chapter.url },
                                                )
                                            }
                                    }
                                if (items.isEmpty()) {
                                    State.Empty
                                } else {
                                    State.Ready(items.toImmutableList())
                                }
                            }
                        },
                    )
                }
            }
            .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(5_000), State.Loading)
    }

    internal sealed interface State {
        data object Loading : State

        data object Empty : State

        data class Ready(val items: kotlinx.collections.immutable.ImmutableList<AnimeLauncherItem>) : State
    }

    internal data class AnimeLauncherItem(
        val entry: tachiyomi.domain.entry.model.Entry,
        val chapter: tachiyomi.domain.entry.model.EntryChapter,
        val animeTitle: String,
        val episodeName: String,
    )
}
