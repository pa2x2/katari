package eu.kanade.tachiyomi.ui.translator

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.translator.session.TranslatorViewModel
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Translate
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data object TranslatorTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 5u,
            title = stringResource(MR.strings.translator_title),
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<TranslatorViewModel, TranslatorViewModel.Factory> {
            create(initialText = "")
        }
        TranslatorRoute(
            viewModel = viewModel,
            onNavigateUp = null,
            onOpenSettings = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
            },
        )
    }
}
