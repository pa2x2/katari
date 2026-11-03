package eu.kanade.tachiyomi.ui.entry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.domain.entry.model.hasCustomCover
import eu.kanade.presentation.entry.components.EntryCoverPage
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.SourceImage
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverDocuments
import eu.kanade.tachiyomi.ui.entry.cover.EntryCoverExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.core.platform.PlatformContext
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID
import tachiyomi.domain.storage.DocumentUri
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class EntryCoverViewModel(
    @Assisted private val entryId: Long,
    private val getEntry: GetEntry,
    private val coverExport: EntryCoverExport,
    private val coverDocuments: EntryCoverDocuments,
    private val coverCache: CoverCache,
    private val entryRepository: EntryRepository,
    private val updateEntry: UpdateEntry,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppUiScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(entryId: Long): EntryCoverViewModel
    }

    val snackbarHostState = SnackbarHostState()

    val state: StateFlow<Entry?> = getEntry.subscribe(entryId)
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5.seconds), null)

    val isSharingAvailable: Boolean
        get() = coverExport.isSharingAvailable

    /** True where Save asks for the file to save to, named after [documentName], and saves through [saveImageTo]. */
    val savesToPickedDocument: Boolean
        get() = coverExport.savesToPickedDocument

    /** False for a local entry whose cover lives where the app can't write. */
    fun isCoverReplaceable(entry: Entry): Boolean =
        entry.source != LOCAL_SOURCE_ID || coverDocuments.editsLocalCovers

    fun hasCustomCover(entry: Entry): Boolean = entry.hasCustomCover(coverCache)

    fun documentName(page: EntryCoverPage): String = "${imageOf(page)?.name ?: "cover"}.jpg"

    fun saveImage(context: PlatformContext, page: EntryCoverPage) {
        save(context, page) { data, name -> coverExport.saveToPictures(context, data, name) }
    }

    fun saveImageTo(context: PlatformContext, page: EntryCoverPage, document: DocumentUri) {
        save(context, page) { data, _ -> coverExport.saveToDocument(context, data, document) }
    }

    private fun save(context: PlatformContext, page: EntryCoverPage, write: suspend (data: Any, name: String) -> Unit) {
        viewModelScope.launch {
            try {
                imageOf(page)?.let { write(it.data, it.name) }
                snackbarHostState.showSnackbar(
                    context.stringResource(
                        when (page) {
                            EntryCoverPage.Cover -> MR.strings.cover_saved
                            EntryCoverPage.Art -> MR.strings.picture_saved
                        },
                    ),
                    withDismissAction = true,
                )
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                snackbarHostState.showSnackbar(
                    context.stringResource(
                        when (page) {
                            EntryCoverPage.Cover -> MR.strings.error_saving_cover
                            EntryCoverPage.Art -> MR.strings.error_saving_picture
                        },
                    ),
                    withDismissAction = true,
                )
            }
        }
    }

    fun shareImage(context: PlatformContext, page: EntryCoverPage) {
        viewModelScope.launch {
            try {
                val image = imageOf(page) ?: return@launch
                coverExport.share(context, image.data, image.name)
            } catch (e: Throwable) {
                logcat(LogPriority.ERROR, e)
                snackbarHostState.showSnackbar(
                    context.stringResource(
                        when (page) {
                            EntryCoverPage.Cover -> MR.strings.error_sharing_cover
                            EntryCoverPage.Art -> MR.strings.error_sharing_picture
                        },
                    ),
                    withDismissAction = true,
                )
            }
        }
    }

    /** The cover or background art shown on [page], with the name a file of it is saved under. */
    private fun imageOf(page: EntryCoverPage): CoverImage? {
        val entry = state.value ?: return null
        return when (page) {
            EntryCoverPage.Cover -> CoverImage(entry.asEntryCover(), entry.title)
            EntryCoverPage.Art -> {
                val url = entry.backgroundUrl ?: return null
                CoverImage(SourceImage(entry.source, url), "${entry.title} (art)")
            }
        }
    }

    /**
     * Replaces the cover with the picked [document]: a local entry's own cover file, or a library entry's custom
     * one.
     */
    fun editCover(context: PlatformContext, document: DocumentUri) {
        val entry = state.value ?: return
        viewModelScope.launchIO {
            coverDocuments.open(document)?.use {
                try {
                    if (entry.source == LOCAL_SOURCE_ID) {
                        coverDocuments.replaceLocalCover(entry, it)
                        updateEntry.awaitUpdateCoverLastModified(entry.id)
                    } else if (entry.favorite) {
                        coverCache.setCustomCoverToCache(entry, it)
                        updateEntry.awaitUpdateCoverLastModified(entry.id)
                    }
                    notifyCoverUpdated(context)
                } catch (e: Exception) {
                    notifyFailedCoverUpdate(context, e)
                }
            }
        }
    }

    fun deleteCustomCover(context: PlatformContext) {
        viewModelScope.launchIO {
            try {
                coverCache.deleteCustomCover(entryId)
                entryRepository.update(
                    entryRepository.getEntryById(entryId)!!
                        .copy(coverLastModified = Instant.now().toEpochMilli()),
                )
                notifyCoverUpdated(context)
            } catch (e: Exception) {
                notifyFailedCoverUpdate(context, e)
            }
        }
    }

    fun showAddToLibraryFirst(context: PlatformContext) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.notification_first_add_to_library),
                withDismissAction = true,
            )
        }
    }

    private fun notifyCoverUpdated(context: PlatformContext) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.cover_updated),
                withDismissAction = true,
            )
        }
    }

    private fun notifyFailedCoverUpdate(context: PlatformContext, e: Throwable) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.notification_cover_update_failed),
                withDismissAction = true,
            )
            logcat(LogPriority.ERROR, e)
        }
    }
}

private class CoverImage(val data: Any, val name: String)
