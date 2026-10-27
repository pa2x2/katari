package eu.kanade.presentation.more.settings.screen.debug

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkQuery
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.rememberDateTimeFormatter
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.AppUiScope
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import java.time.format.DateTimeFormatter
import kotlin.time.Instant

class WorkerInfoScreen : Screen() {

    companion object {
        const val TITLE = "Worker info"
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow

        val viewModel = metroViewModel<Model>()
        val dateTimeFormat = rememberDateTimeFormatter()
        LaunchedEffect(dateTimeFormat) { viewModel.dateTimeFormat.value = dateTimeFormat }
        val enqueued by viewModel.enqueued.collectAsState()
        val finished by viewModel.finished.collectAsState()
        val running by viewModel.running.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = TITLE,
                    navigateUp = navigator::pop,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_copy_to_clipboard),
                                    icon = MaterialSymbols.Rounded.ContentCopy,
                                    onClick = {
                                        context.copyToClipboard(TITLE, enqueued + finished + running)
                                    },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding + PaddingValues(horizontal = KatariTheme.spacing.l)) {
                item { SectionTitle(title = "Enqueued") }
                item { SectionText(text = enqueued) }

                item { SectionTitle(title = "Finished") }
                item { SectionText(text = finished) }

                item { SectionTitle(title = "Running") }
                item { SectionText(text = running) }
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            modifier = Modifier.padding(vertical = KatariTheme.spacing.s),
            role = TextRole.SectionTitle,
        )
    }

    @Composable
    private fun SectionText(text: String) {
        Text(
            text = text,
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            role = TextRole.Code,
            softWrap = false,
        )
    }

    @Inject
    @ViewModelKey
    @ContributesIntoMap(AppUiScope::class)
    internal class Model(
        application: Application,
    ) : ViewModel() {
        private val workManager = application.workManager

        /** Set by the screen, which resolves the app's date and time formats; null until it has. */
        val dateTimeFormat = MutableStateFlow<DateTimeFormatter?>(null)

        val finished = workInfos(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED)

        val running = workInfos(WorkInfo.State.RUNNING)

        val enqueued = workInfos(WorkInfo.State.ENQUEUED)

        private fun workInfos(vararg states: WorkInfo.State) = combine(
            workManager.getWorkInfosFlow(WorkQuery.fromStates(*states)),
            dateTimeFormat.filterNotNull(),
            ::constructString,
        )
            .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), "")

        private fun constructString(list: List<WorkInfo>, dateTimeFormat: DateTimeFormatter) = buildString {
            if (list.isEmpty()) {
                appendLine("-")
            } else {
                list.fastForEach { workInfo ->
                    appendLine("Id: ${workInfo.id}")
                    appendLine("Tags:")
                    workInfo.tags.forEach {
                        appendLine(" - $it")
                    }
                    appendLine("State: ${workInfo.state}")
                    if (workInfo.state == WorkInfo.State.ENQUEUED) {
                        val timestamp = Instant.fromEpochMilliseconds(workInfo.nextScheduleTimeMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .toDateTimestampString(dateTimeFormat)
                        appendLine("Next scheduled run: $timestamp")
                        appendLine("Attempt #${workInfo.runAttemptCount + 1}")
                    }
                    appendLine()
                }
            }
        }
    }
}
