package mihon.feature.upcoming

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.opener
import mihon.app.ui.di.LocalAppUiGraph

class UpcomingScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val openEntry = LocalAppUiGraph.current.entryDetailsDestination.opener(navigator)

        val viewModel = metroViewModel<UpcomingViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        when (state.dialog) {
            is UpcomingViewModel.Dialog.FilterSheet -> {
                UpcomingFilterDialog(
                    viewModel = viewModel,
                )
            }

            null -> {}
        }

        UpcomingScreenContent(
            state = state,
            setSelectedYearMonth = viewModel::setSelectedYearMonth,
            onClickUpcoming = openEntry?.let { open -> { open(it.id) } },
            hasActiveFilters = state.hasActiveFilters,
            onClickFilter = viewModel::showFilterDialog,
        )
    }
}
