package eu.kanade.presentation.more.settings.screen.data

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.components.WarningBanner
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.restore.BackupRestoreJob
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import eu.kanade.tachiyomi.util.system.DeviceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.app.di.AppUiScope
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.ui.components.ProfileCheckRow
import mihon.feature.profiles.ui.unlockProfiles
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.RowList
import mihon.presentation.kit.list.CheckRow
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.BottomActionBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.GroupCard
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class RestoreBackupScreen(
    private val uri: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val model = assistedMetroViewModel<RestoreBackupViewModel, RestoreBackupViewModel.Factory> { create(uri) }
        val state by model.state.collectAsState()

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.pref_restore_backup),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    Button(
                        label = stringResource(MR.strings.action_restore),
                        onClick = {
                            scope.launch {
                                if (unlockProfiles(context, state.lockedSelectedProfileNames)) {
                                    model.startRestore()
                                    navigator.pop()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = state.canRestore && state.options.canRestore() &&
                            (state.profiles.isEmpty() || state.selectedProfileUuids.isNotEmpty()),
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

                if (state.canRestore && state.profiles.isNotEmpty()) {
                    item {
                        BackupSection(
                            title = stringResource(MR.strings.profiles_title),
                            rows = state.profiles.map { profile ->
                                @Composable {
                                    ProfileCheckRow(
                                        name = profile.name,
                                        colorSeed = profile.colorSeed,
                                        isLocked = profile.target == RestoreProfileTarget.Locked,
                                        checked = profile.uuid in state.selectedProfileUuids,
                                        onCheckedChange = { model.toggleProfile(profile.uuid, it) },
                                        subtitle = when (profile.target) {
                                            RestoreProfileTarget.New -> stringResource(MR.strings.restore_profile_new)
                                            RestoreProfileTarget.Merge ->
                                                stringResource(MR.strings.restore_profile_merge)
                                            RestoreProfileTarget.Locked ->
                                                stringResource(MR.strings.restore_profile_locked)
                                        },
                                    )
                                }
                            },
                        )
                    }
                }

                if (state.canRestore) {
                    item {
                        BackupSection(
                            rows = RestoreOptions.options.map { option ->
                                @Composable {
                                    CheckRow(
                                        title = stringResource(option.label),
                                        checked = option.getter(state.options),
                                        onCheckedChange = {
                                            model.toggle(option.setter, it)
                                        },
                                    )
                                }
                            },
                        )
                    }
                }

                if (state.error != null) {
                    errorMessageItem(state.error)
                }
            }
        }
    }

    private fun LazyListScope.errorMessageItem(
        error: Any?,
    ) {
        item {
            GroupCard(modifier = Modifier.padding(horizontal = KatariTheme.spacing.m)) {
                SelectionContainer {
                    Column(
                        modifier = Modifier.padding(KatariTheme.spacing.l),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    ) {
                        when (error) {
                            is MissingRestoreComponents -> {
                                Text(stringResource(MR.strings.backup_restore_content_full))
                                if (error.sources.isNotEmpty()) {
                                    RestoreErrorSection(
                                        title = stringResource(MR.strings.backup_restore_missing_sources),
                                        body = error.sources.joinToString(separator = "\n- ", prefix = "- "),
                                    )
                                }
                                if (error.trackers.isNotEmpty()) {
                                    RestoreErrorSection(
                                        title = stringResource(MR.strings.backup_restore_missing_trackers),
                                        body = error.trackers.joinToString(separator = "\n- ", prefix = "- "),
                                    )
                                }
                            }

                            is InvalidRestore -> {
                                RestoreErrorSection(
                                    title = stringResource(MR.strings.invalid_backup_file),
                                    body = error.uri.toString(),
                                )
                                RestoreErrorSection(
                                    title = stringResource(MR.strings.invalid_backup_file_error),
                                    body = error.message,
                                )
                            }

                            else -> Text(error.toString())
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RestoreErrorSection(title: String, body: String) {
        Column {
            Text(title, role = TextRole.SectionTitle)
            Text(body, role = TextRole.Body)
        }
    }
}

@AssistedInject
internal class RestoreBackupViewModel(
    @Assisted private val uri: String,
    private val context: Context,
    private val backupFileValidator: BackupFileValidator,
    private val profileManager: ProfileManager,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(uri: String): RestoreBackupViewModel
    }

    val state: StateFlow<RestoreBackupViewModel.State>
        field = MutableStateFlow<RestoreBackupViewModel.State>(State())

    init {
        viewModelScope.launchIO {
            validate(uri.toUri())
        }
    }

    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        state.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
    }

    fun toggleProfile(uuid: String, selected: Boolean) {
        state.update {
            it.copy(
                selectedProfileUuids = if (selected) {
                    it.selectedProfileUuids + uuid
                } else {
                    it.selectedProfileUuids -
                        uuid
                },
            )
        }
    }

    fun startRestore() {
        val current = state.value
        BackupRestoreJob.start(
            context = context,
            uri = uri.toUri(),
            options = current.options.copy(
                profileUuids = current.selectedProfileUuids.takeIf { current.profiles.isNotEmpty() },
            ),
        )
    }

    private suspend fun validate(uri: Uri) {
        val results = try {
            backupFileValidator.validate(uri)
        } catch (e: Exception) {
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        val deviceProfiles = profileManager.profiles.value.associateBy { it.uuid }
        val profiles = results.profiles.map { backupProfile ->
            val deviceProfile = deviceProfiles[backupProfile.uuid]
            RestoreProfile(
                uuid = backupProfile.uuid,
                name = backupProfile.name,
                colorSeed = backupProfile.colorSeed,
                target = when {
                    deviceProfile == null -> RestoreProfileTarget.New
                    profileManager.profileRequiresAuthNow(deviceProfile.id) -> RestoreProfileTarget.Locked
                    else -> RestoreProfileTarget.Merge
                },
            )
        }
        val allUuids = profiles.mapTo(mutableSetOf(), RestoreProfile::uuid)
        state.update { it.copy(profiles = profiles, selectedProfileUuids = allUuids) }

        if (results.missingSources.isNotEmpty() || results.missingTrackers.isNotEmpty()) {
            setError(
                error = MissingRestoreComponents(uri, results.missingSources, results.missingTrackers),
                canRestore = true,
            )
            return
        }

        setError(error = null, canRestore = true)
    }

    private fun setError(error: Any?, canRestore: Boolean) {
        state.update {
            it.copy(
                error = error,
                canRestore = canRestore,
            )
        }
    }

    @Immutable
    data class State(
        val error: Any? = null,
        val canRestore: Boolean = false,
        val options: RestoreOptions = RestoreOptions(),
        val profiles: List<RestoreProfile> = emptyList(),
        val selectedProfileUuids: Set<String> = emptySet(),
    ) {
        val lockedSelectedProfileNames: List<String>
            get() = profiles
                .filter { it.target == RestoreProfileTarget.Locked && it.uuid in selectedProfileUuids }
                .map(RestoreProfile::name)
    }
}

/** A profile in the backup and what restoring it does on this device. */
@Immutable
internal data class RestoreProfile(
    val uuid: String,
    val name: String,
    val colorSeed: Long,
    val target: RestoreProfileTarget,
)

internal enum class RestoreProfileTarget { New, Merge, Locked }

private data class MissingRestoreComponents(
    val uri: Uri,
    val sources: List<String>,
    val trackers: List<String>,
)

private data class InvalidRestore(
    val uri: Uri? = null,
    val message: String,
)
