package app.katari.backupconversion

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.data.backup.conversion.BackupApp
import eu.kanade.tachiyomi.data.backup.conversion.BackupConverter
import eu.kanade.tachiyomi.data.backup.conversion.BackupIdentity
import eu.kanade.tachiyomi.data.backup.conversion.ConversionReport
import eu.kanade.tachiyomi.data.backup.conversion.ConvertedBackup
import eu.kanade.tachiyomi.data.backup.create.BackupFileNames
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tachiyomi.domain.storage.DocumentUri

/**
 * The steps of bringing another app's backup over: the file is read to name the app that wrote it (D008), converted
 * once the user presses Convert (D015), and the result restored or saved (D017).
 */
@AssistedInject
internal class BringOverViewModel(
    @Assisted file: DocumentUri?,
    private val converter: BackupConverter,
    private val fileNames: BackupFileNames,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(file: DocumentUri?): BringOverViewModel
    }

    val apps: List<BackupApp> = converter.apps

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    private var work: Job? = null

    init {
        file?.let(::choose)
    }

    fun choose(uri: DocumentUri) {
        state.value = State(file = ChosenFile(uri, name = null, size = null), finding = Finding.Reading)
        step {
            val document = runCatching { converter.describe(uri) }.getOrNull()
            state.update { it.copy(file = ChosenFile(uri, document?.name, document?.size)) }
            val finding = try {
                when (val identity = converter.identify(uri)) {
                    BackupIdentity.Katari -> Finding.Katari
                    BackupIdentity.Unsupported -> Finding.Unsupported
                    is BackupIdentity.Foreign -> Finding.Fits(identity.apps)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Finding.Unreadable(e.message.orEmpty())
            }
            val app = (finding as? Finding.Fits)?.apps?.singleOrNull()
            state.update { it.copy(finding = finding, app = app, appDetected = app != null) }
        }
    }

    /** The app the user says wrote the file, which drops a conversion made as another app's. */
    fun pickApp(app: BackupApp) {
        work?.cancel()
        state.update {
            val detected = (it.finding as? Finding.Fits)?.apps?.singleOrNull() == app
            it.copy(app = app, appDetected = detected, conversion = Conversion.Idle, expanded = emptySet())
        }
    }

    fun convert() {
        val current = state.value
        val file = current.file ?: return
        val app = current.app ?: return
        state.update { it.copy(conversion = Conversion.Running) }
        step {
            val conversion = try {
                Conversion.Done(converter.convert(file.uri, app))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Conversion.Failed(e.message.orEmpty())
            }
            state.update { it.copy(conversion = conversion) }
        }
    }

    fun toggle(kind: ConversionReport.Kind) {
        state.update { it.copy(expanded = if (kind in it.expanded) it.expanded - kind else it.expanded + kind) }
    }

    /** The name offered when saving the converted backup. */
    fun savedFileName(): String = fileNames.convertedFileName(state.value.app?.name.orEmpty())

    /** Saves the converted backup at [to]; the error's message when it fails. */
    suspend fun save(to: DocumentUri): String? {
        val converted = (state.value.conversion as? Conversion.Done)?.backup ?: return null
        return try {
            converter.save(converted, to)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message.orEmpty()
        }
    }

    // One step at a time: choosing another file or app drops the work for the one before
    private fun step(block: suspend () -> Unit) {
        work?.cancel()
        work = viewModelScope.launch { block() }
    }

    @Immutable
    data class State(
        val file: ChosenFile? = null,
        val finding: Finding = Finding.Reading,
        /** The app the file is converted as: the one detected, or the one the user picked. */
        val app: BackupApp? = null,
        /** Whether [app] is the app the file was detected as, rather than one the user picked. */
        val appDetected: Boolean = false,
        val conversion: Conversion = Conversion.Idle,
        /** The report rows opened to their lists, kept here so they stay open as the window changes. */
        val expanded: Set<ConversionReport.Kind> = emptySet(),
    )

    /** [name] and [size] (in bytes) are null while they're read or when the file's storage doesn't say. */
    @Immutable
    data class ChosenFile(val uri: DocumentUri, val name: String?, val size: Long?)

    /** What reading the chosen file found. */
    sealed interface Finding {
        data object Reading : Finding

        /** One of [apps] wrote the file; with more than one, the user says which. */
        data class Fits(val apps: List<BackupApp>) : Finding

        /** A Katari backup, which restores as it is. */
        data object Katari : Finding

        /** No app the tool reads wrote it, though the user may still pick one. */
        data object Unsupported : Finding

        /** The file can't be read as a backup at all, as [message] says. */
        data class Unreadable(val message: String) : Finding
    }

    sealed interface Conversion {
        data object Idle : Conversion

        data object Running : Conversion

        data class Done(val backup: ConvertedBackup) : Conversion

        data class Failed(val message: String) : Conversion
    }
}
