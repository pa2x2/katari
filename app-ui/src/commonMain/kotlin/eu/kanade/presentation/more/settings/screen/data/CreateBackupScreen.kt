package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.presentation.kit.files.DocumentAccess
import app.katari.presentation.kit.files.DocumentPickerRequest
import app.katari.presentation.kit.files.rememberDocumentPicker
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.profiles.core.Profile
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.ui.components.ProfileCheckRow
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class CreateBackupScreen : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val backupCreateSchedule = remember(graph) { graph.backupCreateSchedule }
        val backupFileNames = remember(graph) { graph.backupFileNames }
        val profileUnlock = remember(graph) { graph.profileUnlock }
        val backupSystemWarning = remember(graph) { graph.backupSystemWarning }
        val model = viewModel { CreateBackupViewModel(graph.profileManager, backupCreateSchedule) }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        val chooseBackupDir = rememberDocumentPicker(
            DocumentPickerRequest.CreateDocument("application/*", keepAccess = DocumentAccess.ReadWrite),
        ) {
            if (it != null) {
                model.createBackup(it.uri)
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_create_backup),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    Button(
                        label = stringResource(MR.strings.action_create),
                        onClick = {
                            scope.launch {
                                if (!profileUnlock.unlock(context, state.lockedSelectedProfileNames)) return@launch
                                if (!backupCreateSchedule.isManualBackupRunning()) {
                                    if (!chooseBackupDir.launch(backupFileNames.newFileName())) {
                                        messages.show(MR.strings.file_picker_error)
                                    }
                                } else {
                                    messages.show(MR.strings.backup_in_progress)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = state.options.canCreate() &&
                            (state.profiles.isEmpty() || state.selectedProfileIds.isNotEmpty()),
                    )
                }
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                if (backupSystemWarning.applies) {
                    item {
                        NoticeCard(stringResource(MR.strings.restore_miui_warning), NoticeTone.Warning)
                    }
                }

                if (state.profiles.isNotEmpty()) {
                    item {
                        BackupSection(
                            title = stringResource(MR.strings.profiles_title),
                            rows = state.profiles.map { profile ->
                                @Composable {
                                    ProfileCheckRow(
                                        name = profile.name,
                                        colorSeed = profile.colorSeed,
                                        isLocked = profile.isLocked,
                                        checked = profile.id in state.selectedProfileIds,
                                        onCheckedChange = { model.toggleProfile(profile.id, it) },
                                        subtitle = when {
                                            profile.isLocked -> stringResource(MR.strings.backup_profile_locked)
                                            profile.isArchived -> stringResource(MR.strings.profiles_archived)
                                            else -> null
                                        },
                                    )
                                }
                            },
                        )
                    }
                }

                item {
                    BackupSection(
                        title = stringResource(MR.strings.label_library),
                        rows = optionRows(BackupOptions.libraryOptions, state, model),
                    )
                }

                item {
                    BackupSection(
                        title = stringResource(MR.strings.label_settings),
                        rows = optionRows(BackupOptions.settingsOptions, state, model),
                    )
                }
            }
        }
    }

    private fun optionRows(
        options: List<BackupOptions.Entry>,
        state: CreateBackupViewModel.State,
        model: CreateBackupViewModel,
    ): List<@Composable () -> Unit> = options.map { option ->
        @Composable {
            CheckRow(
                title = stringResource(option.label),
                checked = option.getter(state.options),
                onCheckedChange = {
                    model.toggle(option.setter, it)
                },
                enabled = option.enabled(state.options),
            )
        }
    }
}

private class CreateBackupViewModel(
    profileManager: ProfileManager,
    private val backupCreateSchedule: BackupCreateSchedule,
) : ViewModel() {

    val state: StateFlow<CreateBackupViewModel.State>
        field = MutableStateFlow<CreateBackupViewModel.State>(State())

    init {
        val profiles = profileManager.profiles.value
            .takeIf { it.size > 1 }
            .orEmpty()
            .sortedBy(Profile::isArchived)
            .map { profile ->
                BackupProfile(
                    id = profile.id,
                    name = profile.name,
                    colorSeed = profile.colorSeed,
                    isArchived = profile.isArchived,
                    isLocked = profileManager.profileRequiresAuthNow(profile.id),
                )
            }
        state.update {
            State(profiles = profiles, selectedProfileIds = profiles.mapTo(mutableSetOf(), BackupProfile::id))
        }
    }

    fun toggleProfile(id: Long, selected: Boolean) {
        state.update {
            it.copy(selectedProfileIds = if (selected) it.selectedProfileIds + id else it.selectedProfileIds - id)
        }
    }

    fun toggle(setter: (BackupOptions, Boolean) -> BackupOptions, enabled: Boolean) {
        state.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun createBackup(uri: DocumentUri) {
        val current = state.value
        backupCreateSchedule.startNow(
            uri,
            current.options.copy(profileIds = current.selectedProfileIds.takeIf { current.profiles.isNotEmpty() }),
        )
    }

    @Immutable
    data class State(
        val options: BackupOptions = BackupOptions(),
        val profiles: List<BackupProfile> = emptyList(),
        val selectedProfileIds: Set<Long> = emptySet(),
    ) {
        val lockedSelectedProfileNames: List<String>
            get() = profiles.filter { it.isLocked && it.id in selectedProfileIds }.map(BackupProfile::name)
    }
}

/** A profile the backup can include; it's listed only when there is more than one. */
@Immutable
private data class BackupProfile(
    val id: Long,
    val name: String,
    val colorSeed: Long,
    val isArchived: Boolean,
    val isLocked: Boolean,
)
