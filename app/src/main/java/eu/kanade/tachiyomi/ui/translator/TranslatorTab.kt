package eu.kanade.tachiyomi.ui.translator

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
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
            icon = rememberVectorPainter(MaterialSymbols.Rounded.Translate),
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { TranslatorViewModel() }
        TranslatorRoute(
            viewModel = viewModel,
            onNavigateUp = null,
            onOpenSettings = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
            },
        )
    }
}
