package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class DeepLinkScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<DeepLinkViewModel, DeepLinkViewModel.Factory> {
            create(query = query)
        }
        val state by viewModel.state.collectAsState()
        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.action_search_hint),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            when (state) {
                is DeepLinkViewModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkViewModel.State.NoResults -> {
                    navigator.replace(GlobalSearchScreen(query))
                }
                is DeepLinkViewModel.State.Result -> {
                    val resultState = state as DeepLinkViewModel.State.Result
                    val entry = resultState.entry
                    if (resultState.chapterId == null) {
                        navigator.replace(
                            EntryScreen(
                                entryId = entry.id,
                                fromSource = true,
                            ),
                        )
                    } else if (!viewModel.canOpen(entry)) {
                        navigator.replace(EntryScreen(entryId = entry.id, fromSource = true))
                    } else {
                        LaunchedEffect(resultState.chapterId) {
                            val chapter = viewModel.getChapter(resultState.chapterId)
                            navigator.pop()
                            if (chapter != null) {
                                viewModel.open(context, entry, chapter)
                            }
                        }
                    }
                }
            }
        }
    }
}
