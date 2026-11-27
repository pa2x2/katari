package app.katari.backupconversion

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.backupconversion.BringOverViewModel.Conversion
import app.katari.backupconversion.BringOverViewModel.Finding
import app.katari.core.platform.currentPlatformContext
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Description
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.presentation.kit.files.DocumentPickerRequest
import app.katari.presentation.kit.files.rememberDocumentPicker
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.ChoiceRow
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.list.RowGroupNote
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.BottomActionBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.progress.LinearProgressIndicator
import app.katari.presentation.kit.surface.NoticeCard
import app.katari.presentation.kit.surface.NoticeTone
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.more.settings.screen.data.RestoreBackupScreen
import eu.kanade.presentation.more.settings.screen.data.rememberBackupFilePicker
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.conversion.BackupApp
import kotlinx.coroutines.launch
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import tachiyomi.presentation.core.i18n.fileSize
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Brings another app's backup over (D014-D017): choose the file, check or change the app it was detected as, convert,
 * read what comes over, then restore it through the restore screen or save it as a Katari backup. Opened on [file]
 * when another screen already has it, such as the restore screen pointing a foreign backup here (D002).
 */
class BringOverScreen(private val file: DocumentUri? = null) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val graph = LocalAppUiGraph.current
        val context = currentPlatformContext()
        val restoreRunner = remember(graph) { graph.backupRestoreRunner }
        val messages = rememberScreenMessages()
        val scope = rememberCoroutineScope()
        val model = assistedMetroViewModel<BringOverViewModel, BringOverViewModel.Factory> { create(file) }
        val state by model.state.collectAsState()
        var changingApp by rememberSaveable { mutableStateOf(false) }

        val chooseFile = rememberBackupFilePicker(mimeType = "*/*") { uri ->
            if (uri == null) messages.show(MR.strings.file_null_uri_error) else model.choose(uri)
        }
        val saveFile = rememberDocumentPicker(DocumentPickerRequest.CreateDocument("application/*")) { pick ->
            if (pick != null) {
                scope.launch {
                    val error = model.save(pick.uri)
                    if (error == null) {
                        messages.show(MR.strings.backup_conversion_saved)
                    } else {
                        messages.show(context.stringResource(MR.strings.backup_conversion_save_failed, error))
                    }
                }
            }
        }
        val restore = { uri: DocumentUri ->
            scope.launch {
                if (restoreRunner.isRunning()) {
                    messages.show(MR.strings.restore_in_progress)
                } else {
                    navigator.push(RestoreBackupScreen(uri))
                }
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.backup_conversion_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
            bottomBar = {
                BottomActionBar {
                    val conversion = state.conversion
                    val file = state.file
                    when {
                        file == null || state.finding is Finding.Unreadable -> Button(
                            label = stringResource(MR.strings.backup_conversion_choose_file),
                            onClick = chooseFile,
                            modifier = Modifier.weight(1f),
                        )
                        state.finding == Finding.Katari -> Button(
                            label = stringResource(MR.strings.action_restore),
                            onClick = { restore(file.uri) },
                            modifier = Modifier.weight(1f),
                        )
                        conversion is Conversion.Done -> {
                            Button(
                                label = stringResource(MR.strings.backup_conversion_save),
                                onClick = {
                                    if (!saveFile.launch(model.savedFileName())) {
                                        messages.show(MR.strings.file_picker_error)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                style = ButtonStyle.Outlined,
                            )
                            Button(
                                label = stringResource(MR.strings.action_restore),
                                onClick = { restore(conversion.backup.uri) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        else -> Button(
                            label = stringResource(MR.strings.backup_conversion_convert),
                            onClick = model::convert,
                            modifier = Modifier.weight(1f),
                            enabled = state.app != null && conversion != Conversion.Running &&
                                state.finding != Finding.Reading,
                        )
                    }
                }
            },
        ) { contentPadding ->
            RowList(contentPadding = contentPadding) {
                val file = state.file
                if (file == null) {
                    startItems(model.apps)
                } else {
                    fileItems(
                        state = state,
                        onChooseFile = chooseFile,
                        onChangeApp = { changingApp = true },
                        onPickApp = model::pickApp,
                    )
                    (state.conversion as? Conversion.Done)?.let {
                        conversionReportItems(it.backup.report, state.expanded, model::toggle)
                    }
                }
            }
        }

        if (changingApp) {
            BackupAppDialog(
                apps = model.apps,
                selected = state.app,
                detected = (state.finding as? Finding.Fits)?.apps?.singleOrNull(),
                onPick = {
                    model.pickApp(it)
                    changingApp = false
                },
                onDismissRequest = { changingApp = false },
            )
        }
    }
}

/** Before a file is chosen: what the tool does and the apps it reads, each family with its forks (D014). */
private fun LazyListScope.startItems(apps: List<BackupApp>) {
    item(key = "intro") { RowGroupNote(stringResource(MR.strings.backup_conversion_intro)) }
    item(key = "apps-header") { RowGroupHeader(stringResource(MR.strings.backup_conversion_apps)) }
    val families = apps.groupBy { it.format }.values.toList()
    rowGroupItems(families, key = { it.first().name }) { family ->
        SettingsRow(
            title = family.first().name,
            subtitle = family.drop(1).takeIf { it.isNotEmpty() }
                ?.let { forks -> stringResource(MR.strings.backup_conversion_also, forks.joinToString { it.name }) },
        )
    }
}

/** The chosen file, the app it was detected as, and what stands before converting it. */
private fun LazyListScope.fileItems(
    state: BringOverViewModel.State,
    onChooseFile: () -> Unit,
    onChangeApp: () -> Unit,
    onPickApp: (BackupApp) -> Unit,
) {
    val file = state.file ?: return
    val finding = state.finding
    val fits = (finding as? Finding.Fits)?.apps.orEmpty()
    val asking = fits.size > 1 && !fits.contains(state.app)
    rowGroupItems(listOfNotNull("file", "app".takeIf { finding.offersApps && !asking }), key = { it }) { row ->
        when (row) {
            "file" -> SettingsRow(
                title = file.name ?: file.uri.value,
                subtitle = file.size?.let { fileSize(it) },
                icon = MaterialSymbols.Rounded.Description,
                onClick = onChooseFile,
            )
            else -> SettingsRow(
                title = state.app?.name ?: stringResource(MR.strings.backup_conversion_no_app),
                subtitle = when {
                    state.app == null -> null
                    state.appDetected -> stringResource(MR.strings.backup_conversion_detected)
                    else -> stringResource(MR.strings.backup_conversion_chosen)
                },
                trailing = {
                    Button(
                        label = stringResource(MR.strings.backup_conversion_change),
                        onClick = onChangeApp,
                        style = ButtonStyle.Text,
                    )
                },
            )
        }
    }
    item(key = "file-gap") { RowGroupGap() }
    when {
        finding == Finding.Reading || state.conversion == Conversion.Running -> item(key = "working") {
            LinearProgressIndicator()
        }
        finding == Finding.Katari -> item(key = "katari") {
            NoticeCard(stringResource(MR.strings.backup_conversion_is_katari), NoticeTone.Info)
        }
        finding == Finding.Unsupported && state.app == null -> item(key = "unsupported") {
            NoticeCard(stringResource(MR.strings.backup_conversion_unsupported), NoticeTone.Warning)
        }
        finding is Finding.Unreadable -> item(key = "unreadable") {
            NoticeCard(finding.message, NoticeTone.Warning)
        }
        asking -> {
            item(key = "which") {
                NoticeCard(
                    stringResource(
                        MR.strings.backup_conversion_which_app_text,
                        fits.map { it.name }.reduce { a, b -> stringResource(MR.strings.backup_conversion_or, a, b) },
                    ),
                    NoticeTone.Info,
                )
            }
            item(key = "which-gap") { RowGroupGap() }
            rowGroupItems(fits, key = { "fits-${it.name}" }) { app ->
                ChoiceRow(title = app.name, selected = false, onClick = { onPickApp(app) })
            }
        }
    }
    (state.conversion as? Conversion.Failed)?.let { failed ->
        item(key = "failed") {
            NoticeCard(stringResource(MR.strings.backup_conversion_failed, failed.message), NoticeTone.Warning)
        }
    }
}

/** Whether the file may be converted as an app the user picks: a file no format reads still may (D008). */
private val Finding.offersApps: Boolean
    get() = this is Finding.Fits || this == Finding.Unsupported
