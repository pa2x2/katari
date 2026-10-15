package eu.kanade.tachiyomi.ui.browse.extension.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionDetailsScreen
import eu.kanade.presentation.util.Screen
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import tachiyomi.i18n.*

data class ExtensionDetailsScreen(
    private val pkgName: String,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val viewModel = viewModel { ExtensionDetailsViewModel(pkgName = pkgName, context = context) }
        val state by viewModel.state.collectAsStateWithLifecycle()

        val navigator = LocalNavigator.currentOrThrow

        when (val state = state) {
            ExtensionDetailsState.Loading -> LoadingScreen()
            ExtensionDetailsState.Uninstalled -> {
                LaunchedEffect(Unit) { navigator.pop() }
                EmptyScreen(MR.strings.empty_screen)
            }
            is ExtensionDetailsState.Success -> {
                ExtensionDetailsScreen(
                    navigateUp = navigator::pop,
                    state = state,
                    onClickSourcePreferences = { navigator.push(SourcePreferencesScreen(it)) },
                    onClickEnableAll = { viewModel.toggleSources(true) },
                    onClickDisableAll = { viewModel.toggleSources(false) },
                    onClickClearCookies = viewModel::clearCookies,
                    onClickUninstall = viewModel::uninstallExtension,
                    onClickSource = viewModel::toggleSource,
                    onClickIncognito = viewModel::toggleIncognito,
                )
            }
        }
    }
}
