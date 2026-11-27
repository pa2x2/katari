package eu.kanade.presentation.more.settings.screen.data

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.backupconversion.BringOverScreen
import app.katari.core.platform.currentPlatformContext
import app.katari.missingextensions.model.MissingExtension
import app.katari.missingextensions.restore.BackupMissingExtensions
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.CheckRow
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.progress.LoadingIndicator
import app.katari.presentation.kit.surface.GroupCard
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.profiles.core.Profile
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.core.unusedName
import app.katari.profiles.ui.components.ProfileCheckRow
import app.katari.sync.client.status.SyncStatus
import app.katari.sync.ui.status.SyncStatusState
import app.katari.ui.di.AppUiScope
import app.katari.ui.di.LocalAppUiGraph
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
import eu.kanade.domain.base.OnboardingPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.BackupFileValidator
import eu.kanade.tachiyomi.data.backup.conversion.BackupConverter
import eu.kanade.tachiyomi.data.backup.conversion.BackupIdentity
import eu.kanade.tachiyomi.data.backup.storage.BackupReadProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.backup.model.RestoreOptions
import tachiyomi.domain.backup.service.BackupRestoreRunner
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.random.Random

class RestoreBackupScreen(
    private val uri: DocumentUri,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = currentPlatformContext()
        val graph = LocalAppUiGraph.current
        val profileUnlock = remember(graph) { graph.profileUnlock }
        val backupSystemWarning = remember(graph) { graph.backupSystemWarning }
        val scope = rememberCoroutineScope()
        val model = assistedMetroViewModel<RestoreBackupViewModel, RestoreBackupViewModel.Factory> { create(uri) }
        val state by model.state.collectAsState()
        val deviceProfiles by model.deviceProfiles.collectAsState()
        var confirmRollBack by rememberSaveable { mutableStateOf(false) }
        var choosingProfile by rememberSaveable { mutableStateOf(false) }
        val restore = {
            scope.launch {
                if (profileUnlock.unlock(context, state.lockedSelectedProfileNames)) {
                    model.startRestore()
                    // A converted backup goes back past the tool too, to where it was opened from, such as the
                    // first-run guide, which goes on while the restore runs.
                    navigator.popUntil { it !is RestoreBackupScreen && it !is BringOverScreen }
                }
            }
        }

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
                    val identity = state.identity
                    if (identity != null && identity != BackupIdentity.Katari) {
                        Button(
                            label = (identity as? BackupIdentity.Foreign)?.apps?.singleOrNull()
                                ?.let { stringResource(MR.strings.backup_conversion_bring_over_from, it.name) }
                                ?: stringResource(MR.strings.backup_conversion_title),
                            onClick = { navigator.replace(BringOverScreen(uri)) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Button(
                            label = stringResource(MR.strings.action_restore),
                            onClick = { if (state.options.rollBackAccount) confirmRollBack = true else restore() },
                            modifier = Modifier.weight(1f),
                            enabled = state.canRestore && state.options.canRestore() &&
                                (state.profiles.isEmpty() || state.selectedProfileUuids.isNotEmpty()),
                        )
                    }
                }
            },
        ) { contentPadding ->
            if (state.reading) {
                // D102
                val progress = state.readProgress
                LoadingScreen(
                    modifier = Modifier.padding(contentPadding),
                    message = listOfNotNull(
                        stringResource(MR.strings.restore_reading_backup),
                        progress?.let { backupReadAmount(it) },
                    ).joinToString("\n"),
                    progress = progress?.let { { it.fraction } },
                )
                return@Scaffold
            }
            RowList(contentPadding = contentPadding) {
                if (backupSystemWarning.applies) {
                    item {
                        NoticeCard(stringResource(MR.strings.restore_miui_warning), NoticeTone.Warning)
                    }
                }

                when (val identity = state.identity) {
                    is BackupIdentity.Foreign -> item {
                        val apps = identity.apps.map { it.name }
                            .reduce { a, b -> stringResource(MR.strings.backup_conversion_or, a, b) }
                        NoticeCard(stringResource(MR.strings.restore_other_app_backup, apps), NoticeTone.Info)
                    }
                    BackupIdentity.Unsupported -> item {
                        NoticeCard(stringResource(MR.strings.restore_unknown_backup), NoticeTone.Info)
                    }
                    BackupIdentity.Katari, null -> Unit
                }

                if (state.canRestore && state.syncLinked) {
                    item {
                        RestoreReachSection(
                            createdAt = state.createdAt,
                            rollBack = state.options.rollBackAccount,
                            onRollBackChange = model::setRollBackAccount,
                        )
                    }
                }

                if (state.options.rollBackAccount) {
                    item { RowGroupNote(stringResource(MR.strings.sync_rollback_needs_everything)) }
                }

                val into = state.into
                if (state.canRestore && into != null && !state.options.rollBackAccount) {
                    item { RestoreIntoSection(into, onClick = { choosingProfile = true }) }
                }

                if (state.canRestore && state.profiles.isNotEmpty() && !state.options.rollBackAccount) {
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

                if (state.canRestore && !state.options.rollBackAccount) {
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

                if (state.checkingExtensions) {
                    rowGroupItems(listOf(Unit), key = { "checking-extensions" }) {
                        SettingsRow(
                            title = stringResource(MR.strings.restore_checking_extensions),
                            leading = { LoadingIndicator() },
                        )
                    }
                }

                if (state.error != null) {
                    errorMessageItem(state.error)
                }
            }
        }

        val into = state.into
        if (choosingProfile && into != null) {
            RestoreIntoDialog(
                into = into,
                profiles = deviceProfiles,
                onPick = {
                    model.setIntoNewProfile(it)
                    choosingProfile = false
                },
                onRename = {
                    model.renameNewProfile(it)
                    choosingProfile = false
                },
                onDismissRequest = { choosingProfile = false },
            )
        }

        if (confirmRollBack) {
            RollBackConfirmDialog(
                createdAt = state.createdAt,
                onConfirm = {
                    confirmRollBack = false
                    restore()
                },
                onDismissRequest = { confirmRollBack = false },
            )
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
                                val (installable, unavailable) = error.extensions.partition { it.cantInstall == null }
                                Text(
                                    stringResource(
                                        if (error.extensions.isEmpty()) {
                                            MR.strings.backup_restore_content_full
                                        } else {
                                            MR.strings.missing_extensions_restore_note
                                        },
                                    ),
                                )
                                if (installable.isNotEmpty()) {
                                    RestoreErrorSection(
                                        title = stringResource(MR.strings.missing_extensions_restore_installable),
                                        body = installable.lines(),
                                    )
                                }
                                if (unavailable.isNotEmpty()) {
                                    RestoreErrorSection(
                                        title = stringResource(MR.strings.missing_extensions_unavailable),
                                        body = unavailable.lines(),
                                    )
                                }
                            }

                            is InvalidRestore -> {
                                RestoreErrorSection(
                                    title = stringResource(MR.strings.invalid_backup_file),
                                    body = error.uri.value,
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
    private fun List<MissingExtension>.lines(): String = map {
        val count = it.titleCount.toInt()
        // Sources only entries outside the library come from have nothing to count
        if (count == 0) return@map it.name
        stringResource(
            MR.strings.missing_extensions_restore_line,
            it.name,
            pluralStringResource(MR.plurals.missing_extensions_titles, count, count),
        )
    }.joinToString(separator = "\n")

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
    @Assisted private val uri: DocumentUri,
    private val backupRestoreRunner: BackupRestoreRunner,
    private val backupFileValidator: BackupFileValidator,
    private val backupConverter: BackupConverter,
    private val backupMissingExtensions: BackupMissingExtensions,
    private val profileManager: ProfileManager,
    private val onboardingPreferences: OnboardingPreferences,
    syncStatus: SyncStatusState,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(uri: DocumentUri): RestoreBackupViewModel
    }

    val state: StateFlow<RestoreBackupViewModel.State>
        field = MutableStateFlow<RestoreBackupViewModel.State>(State())

    val deviceProfiles: StateFlow<List<Profile>> = profileManager.profiles

    init {
        viewModelScope.launchIO {
            validate(uri)
        }
        viewModelScope.launch {
            syncStatus.status.filterNotNull().collect { status ->
                val linked = status.link is SyncStatus.Link.Linked
                state.update {
                    it.copy(
                        syncLinked = linked,
                        options = it.options.copy(rollBackAccount = it.options.rollBackAccount && linked),
                    )
                }
            }
        }
    }

    /** An account rollback restores every part of every profile, so choosing it selects them all. */
    fun setRollBackAccount(rollBack: Boolean) {
        state.update {
            it.copy(
                options = RestoreOptions(rollBackAccount = rollBack),
                selectedProfileUuids = it.profiles.mapTo(mutableSetOf(), RestoreProfile::uuid),
            )
        }
    }

    fun toggle(setter: (RestoreOptions, Boolean) -> RestoreOptions, enabled: Boolean) {
        state.update {
            it.copy(
                options = setter(it.options, enabled),
            )
        }
        showMissingExtensions()
    }

    private var missingExtensions: BackupMissingExtensions.Checked? = null

    /** What's missing, as restoring with the current options leaves it: the backup's stores may list extensions. */
    private fun showMissingExtensions() {
        val missing = missingExtensions ?: return
        val extensions = missing.extensions(withBackupStores = state.value.options.extensionStores)
        setError(MissingRestoreComponents(uri, extensions), canRestore = true)
    }

    fun setIntoNewProfile(intoNewProfile: Boolean) {
        state.update { it.copy(into = it.into?.copy(intoNewProfile = intoNewProfile)) }
    }

    fun renameNewProfile(name: String) {
        state.update { it.copy(into = it.into?.copy(newProfileName = name, intoNewProfile = true)) }
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

    suspend fun startRestore() {
        val current = state.value
        // Made only now, so leaving the screen leaves no profile behind (D050)
        val newProfile = current.into?.takeIf { it.intoNewProfile && !current.options.rollBackAccount }
            ?.let {
                profileManager.createProfile(
                    name = deviceProfiles.value.unusedName(it.newProfileName),
                    colorSeed = it.newProfileColorSeed,
                    hideInstalledSources = false,
                )
            }
        backupRestoreRunner.start(
            uri = uri,
            options = if (current.options.rollBackAccount) {
                current.options
            } else {
                current.options.copy(
                    profileUuids = current.selectedProfileUuids.takeIf { current.profiles.isNotEmpty() },
                    newProfileId = newProfile?.id,
                )
            },
        )
    }

    private suspend fun validate(uri: DocumentUri) {
        val onProgress = { progress: BackupReadProgress -> state.update { it.copy(readProgress = progress) } }
        val identity = try {
            backupConverter.identify(uri, onProgress)
        } catch (e: Exception) {
            state.update { it.copy(reading = false) }
            setError(error = InvalidRestore(uri, e.message.toString()), canRestore = false)
            return
        }
        // Restore reads Katari backups only; another app's is brought over through the tool (D002)
        if (identity != BackupIdentity.Katari) {
            state.update { it.copy(identity = identity, reading = false) }
            return
        }
        state.update { it.copy(identity = identity, readProgress = null) }

        val results = try {
            backupFileValidator.validate(uri, onProgress)
        } catch (e: Exception) {
            state.update { it.copy(reading = false) }
            setError(
                error = InvalidRestore(uri, e.message.toString()),
                canRestore = false,
            )
            return
        }

        val deviceProfilesByUuid = profileManager.profiles.value.associateBy { it.uuid }
        val profiles = results.profiles.map { backupProfile ->
            val deviceProfile = deviceProfilesByUuid[backupProfile.uuid]
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
        // On first run the profile in use is the one being set up, so the backup goes into it without asking (D049)
        val inUse = profileManager.activeProfile.value?.takeIf { onboardingPreferences.shownOnboardingFlow.get() }
        val into = results.convertedFrom?.let { app ->
            inUse?.let {
                RestoreInto(
                    inUse = it,
                    intoNewProfile = false,
                    newProfileName = deviceProfiles.value.unusedName(app),
                    newProfileColorSeed = Random.nextLong(0x00FFFFFF),
                )
            }
        }
        // The options show once the file is read; the extension check only fills the note under them (D103)
        state.update {
            it.copy(
                reading = false,
                profiles = profiles,
                selectedProfileUuids = allUuids,
                createdAt = results.createdAt,
                into = into,
                checkingExtensions = results.missingSources.isNotEmpty(),
            )
        }
        setError(error = null, canRestore = true)

        if (results.missingSources.isNotEmpty()) {
            missingExtensions = backupMissingExtensions.check(results.missingSources, results.extensionStores)
            state.update { it.copy(checkingExtensions = false) }
            showMissingExtensions()
        }
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
        /** When the backup was made, in epoch milliseconds; 0 when it doesn't say. */
        val createdAt: Long = 0,
        /** Whether the device syncs, which offers rolling the account back. */
        val syncLinked: Boolean = false,
        /** Which profile a converted backup goes into; null for a Katari backup, which lists its own profiles. */
        val into: RestoreInto? = null,
        /** Which app wrote the file; null until it's read. Only a Katari backup restores here. */
        val identity: BackupIdentity? = null,
        /** Whether the file is still being read, which the options wait for. */
        val reading: Boolean = true,
        /** How far reading the file has got; null before the first count or when its size isn't known. */
        val readProgress: BackupReadProgress? = null,
        /** Whether the stores' lists are still being read for the extensions the backup's titles need. */
        val checkingExtensions: Boolean = false,
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
    val uri: DocumentUri,
    val extensions: List<MissingExtension>,
)

private data class InvalidRestore(
    val uri: DocumentUri,
    val message: String,
)
