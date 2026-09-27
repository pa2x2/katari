package eu.kanade.presentation.more.settings.screen.textrecognition.models

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Lists every stored model revision, complete or partial, and lets the user reclaim its storage. */
internal class ModelArtifactStorageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { ModelArtifactStorageScreenModel() }
        val stored by model.stored.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.model_artifacts_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val artifacts = stored
            when {
                artifacts == null -> LoadingScreen(Modifier.padding(contentPadding))
                artifacts.isEmpty() -> EmptyScreen(
                    stringRes = MR.strings.model_artifacts_empty,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> LazyColumn(contentPadding = contentPadding) {
                    items(artifacts, key = { "${it.descriptor.id.value}@${it.descriptor.revision}" }) { artifact ->
                        ListItem(
                            supportingContent = {
                                val size = formatModelArtifactSize(artifact.storedBytes)
                                Text(
                                    if (artifact.complete) {
                                        stringResource(MR.strings.model_artifacts_state_installed, size)
                                    } else {
                                        stringResource(MR.strings.model_artifacts_state_partial, size)
                                    },
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { model.delete(artifact.descriptor.id) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = stringResource(MR.strings.action_delete),
                                    )
                                }
                            },
                            content = { Text(artifact.descriptor.displayName) },
                        )
                    }
                }
            }
        }
    }
}

internal class ModelArtifactStorageScreenModel(
    private val store: ModelArtifactStore = Injekt.get(),
) : ScreenModel {
    val stored: StateFlow<List<StoredModelArtifact>?> = store.observeStored()
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    fun delete(artifact: ModelArtifactId) {
        screenModelScope.launch { store.delete(artifact) }
    }
}
