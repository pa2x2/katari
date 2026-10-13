package eu.kanade.presentation.more.settings.screen.translation.series

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.SettingsTranslationScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.base.viewmodel.rememberScreenViewModel

internal class SeriesTranslationLanguagesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberSeriesTranslationLanguagesViewModel()
        val series by model.series.collectAsState()

        SeriesTranslationLanguagesContent(
            series = series,
            onBack = navigator::pop,
            onClear = model::clear,
            onClearAll = model::clearAll,
        )
    }
}

/** Shared by Translation settings and the list it opens, so both show the same series. */
@Composable
internal fun rememberSeriesTranslationLanguagesViewModel(): SeriesTranslationLanguagesViewModel =
    SettingsTranslationScreen.rememberScreenViewModel { SeriesTranslationLanguagesViewModel() }
