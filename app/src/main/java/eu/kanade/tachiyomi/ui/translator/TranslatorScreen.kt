package eu.kanade.tachiyomi.ui.translator

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.translator.session.TranslatorViewModel

class TranslatorScreen(
    private val initialText: String = "",
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<TranslatorViewModel, TranslatorViewModel.Factory> {
            create(initialText)
        }
        TranslatorRoute(
            viewModel = viewModel,
            onNavigateUp = navigator::pop,
            onOpenSettings = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
            },
        )
    }
}
