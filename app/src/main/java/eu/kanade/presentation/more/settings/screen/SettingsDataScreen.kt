package eu.kanade.presentation.more.settings.screen

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.core.net.toUri
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.hippo.unifile.UniFile
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.data.CreateBackupScreen
import eu.kanade.presentation.more.settings.screen.data.LibraryExportColumnsDialog
import eu.kanade.presentation.more.settings.screen.data.StorageInfo
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
import eu.kanade.presentation.util.relativeTimeSpanString
import eu.kanade.tachiyomi.data.backup.create.BackupCreateJob
import eu.kanade.tachiyomi.data.export.LibraryExporter
import eu.kanade.tachiyomi.data.export.LibraryExporter.ExportOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.entry.interactions.media.EntryMediaCacheClearResult
import mihon.entry.interactions.media.EntryMediaCacheId
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Help
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.list.ControlRow
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.Constants
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.storage.displayablePath
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.backup.service.BackupPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState

object SettingsDataScreen : SearchableSettings {

    val restorePreferenceKeyString = MR.strings.label_backup
    const val HELP_URL = "${Constants.URL_GUIDE}backups-and-storage/"

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
        val context = LocalContext.current
        val backupPreferences = context.appGraph.backupPreferences
        val storagePreferences = context.appGraph.storagePreferences

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
    ): ManagedActivityResultLauncher<Uri?, Uri?> {
        val context = LocalContext.current
        val messages = rememberScreenMessages()

        return rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                // For some reason InkBook devices do not implement the SAF properly. Persistable URI grants do not
                // work. However, simply retrieving the URI and using it works fine for these devices. Access is not
                // revoked after the app is closed or the device is restarted.
                // This also holds for some Samsung devices. Thus, we simply execute inside of a try-catch block and
                // ignore the exception if it is thrown.
                try {
                    context.contentResolver.takePersistableUriPermission(uri, flags)
                } catch (e: SecurityException) {
                    logcat(LogPriority.ERROR, e)
                    messages.show(MR.strings.file_picker_uri_permission_unsupported)
                }

                UniFile.fromUri(context, uri)?.let {
                    storageDirPref.set(it.uri.toString())
                }
            }
        }
    }

    @Composable
    fun storageLocationText(
        storageDirPref: tachiyomi.core.common.preference.Preference<String>,
    ): String {
        val context = LocalContext.current
        val storageDir by storageDirPref.collectAsState()

        if (storageDir == storageDirPref.defaultValue()) {
            return stringResource(MR.strings.no_location_set)
        }

        val invalidLocation = stringResource(MR.strings.invalid_location, storageDir)
        // Resolving a document provider's path can query the provider
        val location by produceState("", storageDir, invalidLocation) {
            value = withContext(Dispatchers.IO) {
                UniFile.fromUri(context, storageDir.toUri())?.displayablePath
            } ?: invalidLocation
        }
        return location
    }

    @Composable
    private fun getStorageLocationPref(
        storagePreferences: StoragePreferences,
    ): Preference.PreferenceItem.TextPreference {
        val messages = rememberScreenMessages()
        val pickStorageLocation = storageLocationPicker(storagePreferences.baseStorageDirectory)

        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.pref_storage_location),
            subtitle = storageLocationText(storagePreferences.baseStorageDirectory),
            onClick = {
                try {
                    pickStorageLocation.launch(null)
                } catch (e: ActivityNotFoundException) {
                    messages.show(MR.strings.file_picker_error)
                }
            },
        )
    }

    @Composable
    private fun getBackupAndRestoreGroup(backupPreferences: BackupPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
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
                    onValueChanged = {
                        scope.launchNonCancellable { BackupCreateJob.setupTask(context, it) }
                        true
                    },
                ),
                Preference.PreferenceItem.InfoPreference(
                    stringResource(MR.strings.backup_info) + "\n\n" +
                        stringResource(MR.strings.last_auto_backup_info, relativeTimeSpanString(lastAutoBackup)),
                ),
            ),
        )
    }

    @Composable
    private fun getDataGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val mediaCacheFeature = remember { context.appGraph.entryMediaCacheFeature }
        val mediaCacheSettings = remember(mediaCacheFeature) { mediaCacheFeature.settings() }
        var cacheSizeSema by remember { mutableIntStateOf(0) }
        val cacheSizes by produceState(emptyMap<EntryMediaCacheId, String>(), mediaCacheSettings, cacheSizeSema) {
            value = withContext(Dispatchers.IO) {
                mediaCacheSettings.mapNotNull { setting ->
                    mediaCacheFeature.readableSize(setting.id)?.let { setting.id to it }
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
                            subtitle = stringResource(MR.strings.used_cache, cacheSizes[setting.id].orEmpty()),
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

        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val entryRepository = remember { context.appGraph.entryRepository }
        var favorites by remember { mutableStateOf<List<Entry>>(emptyList()) }
        LaunchedEffect(Unit) {
            favorites = entryRepository.getFavorites()
        }

        val saveFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("text/csv"),
        ) { uri ->
            uri?.let {
                scope.launch {
                    LibraryExporter.exportToCsv(
                        context = context,
                        uri = it,
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
