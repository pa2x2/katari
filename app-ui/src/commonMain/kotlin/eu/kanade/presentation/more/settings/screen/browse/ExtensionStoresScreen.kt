package eu.kanade.presentation.more.settings.screen.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.more.settings.screen.browse.components.ExtensionStoreConfirmDialog
import eu.kanade.presentation.more.settings.screen.browse.components.ExtensionStoreCreateDialog
import eu.kanade.presentation.more.settings.screen.browse.components.ExtensionStoreDeleteDialog
import eu.kanade.presentation.more.settings.screen.browse.components.ExtensionStoreUnsignedDialog
import eu.kanade.presentation.more.settings.screen.browse.components.ExtensionStoresScreen
import eu.kanade.presentation.util.Screen

class ExtensionStoresScreen(
    private val url: String? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current

        val viewModel = metroViewModel<ExtensionStoresViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(url) {
            url?.let { viewModel.addFromDeeplink(url) }
        }

        if (state is ExtensionStoreScreenState.Loading) {
            LoadingScreen()
            return
        }

        val successState = state as ExtensionStoreScreenState.Success

        ExtensionStoresScreen(
            state = successState,
            onClickCreate = { viewModel.showDialog(ExtensionStoreDialog.Create()) },
            onCopy = { graph.textClipboard.copy(it.indexUrl, it.indexUrl) },
            onOpenWebsite = { graph.linkOpener.open(context, it.contact.website) },
            onOpenDiscord = { store -> store.contact.discord?.let { graph.linkOpener.open(context, it) } },
            onClickDelete = { viewModel.showDialog(ExtensionStoreDialog.Delete(it)) },
            onClickRefresh = { viewModel.refreshRepos() },
            navigateUp = navigator::pop,
        )

        when (val dialog = successState.dialog) {
            null -> {}
            is ExtensionStoreDialog.Create -> {
                ExtensionStoreCreateDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onCreate = { viewModel.createRepo(it) },
                    storeIndexUrls = successState.stores.map { it.indexUrl }.toSet(),
                    processing = dialog.processing,
                    errorMessage = dialog.errorMessage,
                )
            }
            is ExtensionStoreDialog.Delete -> {
                ExtensionStoreDeleteDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onDelete = { viewModel.deleteRepo(dialog.store.indexUrl) },
                    storeName = dialog.store.name,
                    storeIndexUrl = dialog.store.indexUrl,
                )
            }
            is ExtensionStoreDialog.ConfirmUnsigned -> {
                ExtensionStoreUnsignedDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onAdd = { viewModel.addUnsigned(dialog.store) },
                    storeName = dialog.store.name,
                )
            }
            is ExtensionStoreDialog.Confirm -> {
                ExtensionStoreConfirmDialog(
                    onDismissRequest = viewModel::dismissDialog,
                    onCreate = { viewModel.createRepo(dialog.url) },
                    storeIndexUrl = dialog.url,
                    storeAlreadyExists = dialog.alreadyExists,
                    processing = dialog.processing,
                    errorMessage = dialog.errorMessage,
                )
            }
        }
    }
}
