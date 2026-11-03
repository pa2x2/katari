package eu.kanade.tachiyomi.ui.download

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.TabbedScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.download.translation.TranslationQueueViewModel
import eu.kanade.tachiyomi.ui.download.translation.translationQueueTab
import tachiyomi.i18n.*

/** The download queue, and the translation queue beside it when chapters can be translated in the background. */
object DownloadQueueScreen : Screen() {
    private fun readResolve(): Any = DownloadQueueScreen

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val downloads = metroViewModel<DownloadQueueViewModel>()
        val translations = metroViewModel<TranslationQueueViewModel>()
        TabbedScreen(
            titleRes = MR.strings.label_download_queue,
            tabs = listOfNotNull(
                downloadQueueDownloadsTab(downloads),
                if (translations.isAvailable) translationQueueTab(translations) else null,
            ),
            navigateUp = navigator::pop,
            snackbarHostState = translations.snackbarHostState,
        )
    }
}
