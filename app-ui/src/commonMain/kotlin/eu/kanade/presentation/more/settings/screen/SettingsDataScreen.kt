package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.LibraryExportColumnsDialog
import eu.kanade.presentation.more.settings.screen.data.StorageInfo
import eu.kanade.presentation.more.settings.screen.data.collectPickedStorageDirectory
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
import eu.kanade.presentation.more.settings.screen.data.storageLocationDisplayPath
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.export.LibraryExporter
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import mihon.entry.interactions.media.EntryMediaCacheClearResult
import mihon.entry.interactions.media.EntryMediaCacheId
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.files.DocumentAccess
import mihon.presentation.kit.files.DocumentPicker
import mihon.presentation.kit.files.DocumentPickerRequest
import mihon.presentation.kit.files.rememberDocumentPicker
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import tachiyomi.presentation.core.i18n.fileSize
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsDataScreen : SearchableSettings {

    val restorePreferenceKeyString = MR.strings.label_backup
    const val HELP_URL = "${Constants.URL_DOCS}backups-and-storage/"

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.label_data_storage

    @Composable
    override fun RowScope.AppBarAction() {
        val uriHandler = LocalUriHandler.current
        IconButton(
            icon = MaterialSymbols.AutoMirroredRounded.Help,
            contentDescription = stringResource(MR.strings.onboarding_storage_help_action),
            onClick = { uriHandler.openUri(HELP_URL) },
        )
    }

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalAppUiGraph.current
        val backupPreferences = graph.backupPreferences
        val storagePreferences = graph.storagePreferences

        return listOf(
            getStorageLocationPref(storagePreferences = storagePreferences),
            Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.pref_storage_location_info)),

            getBackupAndRestoreGroup(backupPreferences = backupPreferences),
            getDataGroup(),
            getExportGroup(),
        )
    }

    @Composable
    fun storageLocationPicker(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): DocumentPicker {
        val paths = LocalAppUiGraph.current.storageLocationPaths
        val messages = rememberScreenMessages()

        return rememberDocumentPicker(
            DocumentPickerRequest.OpenFolder(keepAccess = DocumentAccess.ReadWrite),
        ) { pick ->
            if (pick != null) {
                // For some reason InkBook devices do not implement the SAF properly. Persistable URI grants do not
                // work. However, simply retrieving the URI and using it works fine for these devices. Access is not
                // revoked after the app is closed or the device is restarted.
                // This also holds for some Samsung devices. Thus, a grant that can't be kept is only reported, and the
                // folder is used anyway.
                if (!pick.accessKept) {
                    messages.show(MR.strings.file_picker_uri_permission_unsupported)
                }

                paths.locationOf(pick.uri)?.let(storageDirPref::set)
            }
        }
    }

    @Composable
    private fun getStorageLocationPref(
        storagePreferences: StoragePreferences,
    ): Preference.PreferenceItem.TextPreference {
        val messages = rememberScreenMessages()
        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory)
        val pickedLocation = collectPickedStorageDirectory(storagePreferences)

        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.pref_storage_location),
            subtitle = if (pickedLocation == null) {
                stringResource(MR.strings.no_location_set)
            } else {
                storageLocationDisplayPath(pickedLocation)
            },
            onClick = {
                if (!pickStorageLocation.launch()) {
                    messages.show(MR.strings.file_picker_error)
                }
            },
        )
    }

    @Composable
    private fun getBackupAndRestoreGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
        val graph = LocalAppUiGraph.current
        val backupCreateSchedule = remember(graph) { graph.backupCreateSchedule }
        val navigator = LocalNavigator.currentOrThrow

        val scope = rememberCoroutineScope()
        val lastAutoBackup by backupPreferences.lastAutoBackupTimestamp.collectAsState()

        val restoreBackup = rememberRestoreBackupLauncher()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.label_backup),
            preferenceItems = listOf(
                // Manual actions
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(restorePreferenceKeyString),
                ) {
                    ControlRow {
                        Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
                            Button(
                                label = stringResource(MR.strings.pref_create_backup),
                                onClick = { navigator.push(CreateBackupScreen()) },
                                modifier = Modifier.weight(1f),
                                style = ButtonStyle.Outlined,
                            )
                            Button(
                                label = stringResource(MR.strings.pref_restore_backup),
                                onClick = { restoreBackup() },
                                modifier = Modifier.weight(1f),
                                style = ButtonStyle.Outlined,
                            )
                        }
                    }
                },

                // Automatic backups
                Preference.PreferenceItem.ListPreference(
                    preference = backupPreferences.backupInterval,
                    entries = mapOf(
                        0 to stringResource(MR.strings.off),
                        6 to stringResource(MR.strings.update_6hour),
                        12 to stringResource(MR.strings.update_12hour),
                        24 to stringResource(MR.strings.update_24hour),
                        48 to stringResource(MR.strings.update_48hour),
                        168 to stringResource(MR.strings.update_weekly),
                    ),
                    title = stringResource(MR.strings.pref_backup_interval),
                    visible = backupCreateSchedule.isAutomaticBackupOffered,
                    onValueChanged = {
                        scope.launchNonCancellable { backupCreateSchedule.setupTask(it) }
                        true
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    if (backupCreateSchedule.isAutomaticBackupOffered) {
                        stringResource(MR.strings.backup_info) + "\n\n" +
                            stringResource(MR.strings.last_auto_backup_info, relativeTimeSpanString(lastAutoBackup))
                    } else {
                        stringResource(MR.strings.backup_info)
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val context = currentPlatformContext()
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val graph = LocalAppUiGraph.current
        val mediaCacheFeature = remember(graph) { graph.entryMediaCacheFeature }
        val mediaCacheSettings = remember(mediaCacheFeature) { mediaCacheFeature.settings() }
        var cacheSizeSema by remember { mutableIntStateOf(0) }
        val cacheSizes by produceState(emptyMap<EntryMediaCacheId, Long>(), mediaCacheSettings, cacheSizeSema) {
            value = withContext(Dispatchers.IO) {
                mediaCacheSettings.mapNotNull { setting ->
                    mediaCacheFeature.sizeBytes(setting.id)?.let { setting.id to it }
                }.toMap()
            }
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_storage_usage),
            preferenceItems = buildList {
                add(
                    Preference.PreferenceItem.CustomPreference(
                        title = stringResource(MR.strings.pref_storage_usage),
                    ) {
                        ControlRow { StorageInfo() }
                    },
                )

                mediaCacheSettings.forEach { setting ->
                    add(
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(setting.clearLabel),
                            subtitle = stringResource(
                                MR.strings.used_cache,
                                cacheSizes[setting.id]?.let {
                                    fileSize(it)
                                }.orEmpty(),
                            ),
                            onClick = {
                                scope.launchNonCancellable {
                                    when (val result = mediaCacheFeature.clear(setting.id)) {
                                        is EntryMediaCacheClearResult.Cleared -> {
                                            withUIContext {
                                                messages.show(
                                                    context.stringResource(
                                                        MR.strings.cache_deleted,
                                                        result.deletedFiles,
                                                    ),
                                                )
                                                cacheSizeSema++
                                            }
                                        }
                                        is EntryMediaCacheClearResult.Failed -> {
                                            logcat(LogPriority.ERROR, result.error)
                                            withUIContext { messages.show(MR.strings.cache_delete_error) }
                                        }
                                        is EntryMediaCacheClearResult.Inapplicable -> {
                                            withUIContext { messages.show(MR.strings.cache_delete_error) }
                                        }
                                    }
                                }
                            },
                        ),
                    )
                    add(
                        Preference.PreferenceItem.SwitchPreference(
                            preference = setting.autoClearOnLaunch,
                            title = stringResource(setting.autoClearLabel),
                        ),
                    )
                }
            },
        )
    }

    @Composable
    private fun getExportGroup(): Preference.PreferenceGroup {
        var showDialog by remember { mutableStateOf(false) }
        var exportOptions by remember {
            mutableStateOf(
                ExportOptions(
                    includeTitle = true,
                    includeAuthor = true,
                    includeArtist = true,
                ),
            )
        }

        val graph = LocalAppUiGraph.current
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val entryRepository = remember(graph) { graph.entryRepository }
        var favorites by remember { mutableStateOf<List<Entry>>(emptyList()) }
        LaunchedEffect(Unit) {
            favorites = entryRepository.getFavorites()
        }

        val saveFileLauncher = rememberDocumentPicker(DocumentPickerRequest.CreateDocument("text/csv")) { pick ->
            pick?.let {
                scope.launch {
                    LibraryExporter.exportToCsv(
                        documents = graph.libraryExportDocuments,
                        uri = it.uri,
                        favorites = favorites,
                        options = exportOptions,
                        onExportComplete = {
                            scope.launch(Dispatchers.Main) {
                                messages.show(MR.strings.library_exported)
                            }
                        },
                    )
                }
            }
        }

        if (showDialog) {
            LibraryExportColumnsDialog(
                options = exportOptions,
                onConfirm = { options ->
                    exportOptions = options
                    saveFileLauncher.launch("katari_library.csv")
                },
                onDismissRequest = { showDialog = false },
            )
        }

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.export),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.library_list),
                    onClick = { showDialog = true },
                ),
            ),
        )
    }
}
