package eu.kanade.tachiyomi.ui.translator

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.translator.session.TranslatorScreenModel

class TranslatorScreen(
    private val initialText: String = "",
) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { TranslatorScreenModel(initialText) }
        TranslatorRoute(
            screenModel = screenModel,
            onNavigateUp = navigator::pop,
            onOpenSettings = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
            },
        )
    }
}
