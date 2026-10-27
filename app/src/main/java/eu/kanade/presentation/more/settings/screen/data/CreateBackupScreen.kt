package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.util.system.DeviceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.profiles.core.Profile
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.components.ProfileCheckRow
import mihon.feature.profiles.ui.unlockProfiles
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.files.DocumentAccess
import mihon.presentation.kit.files.DocumentPickerRequest
import mihon.presentation.kit.files.rememberDocumentPicker
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.domain.backup.model.BackupOptions
import tachiyomi.domain.backup.service.BackupCreateSchedule
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class CreateBackupScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val backupCreateSchedule = remember { context.appGraph.backupCreateSchedule }
        val model = viewModel { CreateBackupViewModel(context.appGraph.profileManager, backupCreateSchedule) }
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
                                if (!unlockProfiles(context, state.lockedSelectedProfileNames)) return@launch
                                if (!backupCreateSchedule.isManualBackupRunning()) {
                                    if (!chooseBackupDir.launch(BackupCreator.getFilename())) {
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
                if (DeviceUtil.isMiui && DeviceUtil.isMiuiOptimizationDisabled()) {
                    item {
                        WarningBanner(MR.strings.restore_miui_warning)
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
