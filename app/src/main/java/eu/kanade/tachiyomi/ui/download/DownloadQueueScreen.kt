package eu.kanade.tachiyomi.ui.download

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.download.translation.TranslationQueueScreenModel
import eu.kanade.tachiyomi.ui.download.translation.translationQueueTab
import mihon.entry.interactions.translate.EntryTranslateFeature
import tachiyomi.i18n.*
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** The download queue, and the translation queue beside it when chapters can be translated in the background. */
object DownloadQueueScreen : Screen() {
    private fun readResolve(): Any = DownloadQueueScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val downloads = rememberScreenModel { DownloadQueueScreenModel() }
        val translations = rememberScreenModel { TranslationQueueScreenModel() }
        val translationAvailable = remember {
            Injekt.get<EntryTranslateFeature>().let { feature -> EntryType.entries.any(feature::isApplicable) }
        }
        TabbedScreen(
            titleRes = MR.strings.label_download_queue,
            tabs = listOfNotNull(
                downloadQueueDownloadsTab(downloads),
                if (translationAvailable) translationQueueTab(translations) else null,
            ),
            navigateUp = navigator::pop,
            snackbarHostState = translations.snackbarHostState,
        )
    }
}
