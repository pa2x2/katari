package eu.kanade.presentation.more.settings.screen.translation.engine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.translation.TranslationSettingsScreenModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.openInBrowser

internal class TranslationEnginePickerScreen(
    private val model: TranslationSettingsScreenModel,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val playground by model.playground.collectAsState()

        TranslationEnginePickerContent(
            engines = model.engines,
            selected = playground.engine,
            onSelect = { engine ->
                model.setEngine(engine)
                navigator.pop()
            },
            onOpenDocumentation = { context.openInBrowser(it, forceDefaultBrowser = true) },
            onBack = navigator::pop,
        )
    }
}
