package eu.kanade.tachiyomi.ui.deeplink

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import mihon.entry.interactions.EntryOpenInteraction
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DeepLinkScreen(
    val query: String = "",
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val entryOpenInteraction = remember { Injekt.get<EntryOpenInteraction>() }

        val screenModel = rememberScreenModel {
            DeepLinkScreenModel(query = query)
        }
        val state by screenModel.state.collectAsState()
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
                is DeepLinkScreenModel.State.Loading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                is DeepLinkScreenModel.State.NoResults -> {
                    navigator.replace(GlobalSearchScreen(query))
                }
                is DeepLinkScreenModel.State.Result -> {
                    val resultState = state as DeepLinkScreenModel.State.Result
                    val entry = resultState.entry
                    if (resultState.chapterId == null) {
                        navigator.replace(
                            EntryScreen(
                                entryId = entry.id,
                                fromSource = true,
                            ),
                        )
                    } else {
                        navigator.pop()
                        LaunchedEffect(resultState.chapterId) {
                            val chapter = Injekt.get<EntryChapterRepository>().getChapterById(resultState.chapterId)
                            if (chapter != null) {
                                entryOpenInteraction.open(context, entry, chapter)
                            }
                        }
                    }
                }
            }
        }
    }
}
