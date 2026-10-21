package eu.kanade.presentation.more.settings.screen.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.backup.create.BackupCreator
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.toast
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
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

class CreateBackupScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = viewModel { CreateBackupViewModel(context.appGraph.profileManager) }
        val state by model.state.collectAsState()
        val scope = rememberCoroutineScope()

        val chooseBackupDir = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/*"),
        ) {
            if (it != null) {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                model.createBackup(context, it)
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
                                if (!BackupCreateJob.isManualJobRunning(context)) {
                                    try {
                                        chooseBackupDir.launch(BackupCreator.getFilename())
                                    } catch (e: ActivityNotFoundException) {
                                        context.toast(MR.strings.file_picker_error)
                                    }
                                } else {
                                    context.toast(MR.strings.backup_in_progress)
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
            ScrollbarLazyColumn(contentPadding = contentPadding) {
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

private class CreateBackupViewModel(profileManager: ProfileManager) : ViewModel() {

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

    fun createBackup(context: Context, uri: Uri) {
        val current = state.value
        BackupCreateJob.startNow(
            context,
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
