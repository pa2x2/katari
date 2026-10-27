package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Size
import dev.zacsweers.metro.AppScope
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
import eu.kanade.tachiyomi.data.saver.Image
import eu.kanade.tachiyomi.data.saver.ImageSaver
import eu.kanade.tachiyomi.data.saver.Location
import eu.kanade.tachiyomi.util.editCover
import eu.kanade.tachiyomi.util.system.getBitmapOrNull
import eu.kanade.tachiyomi.util.system.toShareIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.AppUiScope
import mihon.presentation.kit.feedback.SnackbarHostState
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.UpdateEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.asEntryCover
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.image.LocalCoverManager
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class EntryCoverViewModel(
    @Assisted private val entryId: Long,
    private val getEntry: GetEntry,
    private val imageSaver: ImageSaver,
    private val coverCache: CoverCache,
    private val entryRepository: EntryRepository,
    private val localCoverManager: LocalCoverManager,
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

    fun hasCustomCover(entry: Entry): Boolean = entry.hasCustomCover(coverCache)

    fun saveImage(context: Context, page: EntryCoverPage) {
        viewModelScope.launch {
            try {
                saveImageInternal(context, page, temp = false)
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

    fun shareImage(context: Context, page: EntryCoverPage) {
        viewModelScope.launch {
            try {
                val uri = saveImageInternal(context, page, temp = true) ?: return@launch
                withUIContext {
                    context.startActivity(uri.toShareIntent(context))
                }
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

    /**
     * Saves the cover or background art shown on [page] to the pictures directory, or with [temp] to the temporary
     * share directory.
     *
     * @return the uri to saved file
     */
    private suspend fun saveImageInternal(context: Context, page: EntryCoverPage, temp: Boolean): Uri? {
        val entry = state.value ?: return null
        val (data, name) = when (page) {
            EntryCoverPage.Cover -> entry.asEntryCover() to entry.title
            EntryCoverPage.Art -> {
                val url = entry.backgroundUrl ?: return null
                SourceImage(entry.source, url) to "${entry.title} (art)"
            }
        }
        val req = ImageRequest.Builder(context)
            .data(data)
            .size(Size.ORIGINAL)
            .build()

        return withIOContext {
            val result = context.imageLoader.execute(req).image?.asDrawable(context.resources)

            // TODO: Handle animated cover
            val bitmap = result?.getBitmapOrNull() ?: return@withIOContext null
            imageSaver.save(
                Image.Cover(
                    bitmap = bitmap,
                    name = name,
                    location = if (temp) Location.Cache else Location.Pictures.create(),
                ),
            )
        }
    }

    /**
     * Update cover with local file.
     *
     * @param context Context.
     * @param data uri of the cover resource.
     */
    fun editCover(context: Context, data: Uri) {
        val entry = state.value ?: return
        viewModelScope.launchIO {
            context.contentResolver.openInputStream(data)?.use {
                try {
                    entry.editCover(localCoverManager, it, updateEntry = updateEntry, coverCache = coverCache)
                    notifyCoverUpdated(context)
                } catch (e: Exception) {
                    notifyFailedCoverUpdate(context, e)
                }
            }
        }
    }

    fun deleteCustomCover(context: Context) {
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

    fun showAddToLibraryFirst(context: Context) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.notification_first_add_to_library),
                withDismissAction = true,
            )
        }
    }

    private fun notifyCoverUpdated(context: Context) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.cover_updated),
                withDismissAction = true,
            )
        }
    }

    private fun notifyFailedCoverUpdate(context: Context, e: Throwable) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(
                context.stringResource(MR.strings.notification_cover_update_failed),
                withDismissAction = true,
            )
            logcat(LogPriority.ERROR, e)
        }
    }
}
