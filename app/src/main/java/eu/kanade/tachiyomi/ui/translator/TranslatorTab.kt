package eu.kanade.tachiyomi.ui.translator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.translator.session.TranslatorScreenModel
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object TranslatorTab : Tab {
    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 5u,
            title = stringResource(MR.strings.translator_title),
            icon = rememberVectorPainter(Icons.Outlined.Translate),
        )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { TranslatorScreenModel() }
        TranslatorRoute(
            screenModel = screenModel,
            onNavigateUp = null,
            onOpenSettings = {
                navigator.push(SettingsScreen(SettingsScreen.Destination.Translation))
            },
        )
    }
}
