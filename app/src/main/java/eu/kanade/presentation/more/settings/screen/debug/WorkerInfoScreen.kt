package eu.kanade.presentation.more.settings.screen.debug

import android.app.Application
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
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
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import mihon.app.di.AppUiScope
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ContentCopy
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
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
            LazyColumn(
                contentPadding = contentPadding + PaddingValues(horizontal = KatariTheme.spacing.l),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
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
            role = TextRole.Code,
            softWrap = false,
        )
    }

    @Inject
    @ViewModelKey
    @ContributesIntoMap(AppUiScope::class)
    internal class Model(
        application: Application,
        private val uiPreferences: UiPreferences,
    ) : ViewModel() {
        private val workManager = application.workManager

        val finished = workManager
            .getWorkInfosFlow(
                WorkQuery.fromStates(WorkInfo.State.SUCCEEDED, WorkInfo.State.FAILED, WorkInfo.State.CANCELLED),
            )
            .map(::constructString)
            .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), "")

        val running = workManager
            .getWorkInfosFlow(WorkQuery.fromStates(WorkInfo.State.RUNNING))
            .map(::constructString)
            .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), "")

        val enqueued = workManager
            .getWorkInfosFlow(WorkQuery.fromStates(WorkInfo.State.ENQUEUED))
            .map(::constructString)
            .stateIn(viewModelScope + Dispatchers.IO, SharingStarted.WhileSubscribed(), "")

        private fun constructString(list: List<WorkInfo>) = buildString {
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
                            .toDateTimestampString(
                                UiPreferences.dateFormat(
                                    uiPreferences.dateFormat.get(),
                                ),
                            )
                        appendLine("Next scheduled run: $timestamp")
                        appendLine("Attempt #${workInfo.runAttemptCount + 1}")
                    }
                    appendLine()
                }
            }
        }
    }
}
