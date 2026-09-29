package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.history.activity.HistoryActivityScreenContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

data class HistoryActivityScreen(
    private val startLocalDate: String,
    private val endLocalDate: String,
    private val typeName: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            HistoryActivityScreenModel(startLocalDate, endLocalDate, typeName)
        }
        val state by screenModel.state.collectAsState()
        val summary by screenModel.summary.collectAsState()
        val locale = LocalConfiguration.current.locales[0]
        val title = remember(startLocalDate, endLocalDate, locale) {
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            val start = LocalDate.parse(startLocalDate)
            val end = LocalDate.parse(endLocalDate)
            if (start == end) start.format(formatter) else "${start.format(formatter)} – ${end.format(formatter)}"
        }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = title,
                    subtitle = screenModel.type?.let { stringResource(it.entryTypePresentation().displayNameLabel) },
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { paddingValues ->
            HistoryActivityScreenContent(
                state = state,
                summary = summary,
                type = screenModel.type,
                types = screenModel.types,
                paddingValues = paddingValues,
                onEntryClick = { navigator.push(EntryScreen(it)) },
                onRetry = screenModel::retry,
                onLoadMore = screenModel::loadMore,
            )
        }
    }
}
