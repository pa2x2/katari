package eu.kanade.tachiyomi.ui.entry.cover

import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.katari.core.platform.currentPlatformContext
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.files.DocumentPickerRequest
import app.katari.presentation.kit.files.rememberDocumentPicker
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.entry.EditCoverAction
import eu.kanade.presentation.entry.components.EntryCoverDialog
import eu.kanade.presentation.entry.components.EntryCoverPage
import eu.kanade.tachiyomi.data.coil.SourceImage
import eu.kanade.tachiyomi.ui.entry.EntryCoverViewModel
import tachiyomi.domain.source.identity.LOCAL_SOURCE_ID

/** The entry's cover and background art full screen, with Share, Save and Edit as the platform offers them. */
@Composable
fun EntryCoverViewer(entryId: Long, onDismissRequest: () -> Unit) {
    val context = currentPlatformContext()
    val sm = assistedMetroViewModel<EntryCoverViewModel, EntryCoverViewModel.Factory> {
        create(entryId)
    }
    val entry by sm.state.collectAsStateWithLifecycle()
    if (entry != null) {
        val getContent = rememberDocumentPicker(DocumentPickerRequest.GetContent("image/*")) {
            if (it == null) return@rememberDocumentPicker
            sm.editCover(context, it.uri)
        }
        var pageToSave by remember { mutableStateOf<EntryCoverPage?>(null) }
        val saveDocument = rememberDocumentPicker(DocumentPickerRequest.CreateDocument("image/jpeg")) {
            val page = pageToSave ?: return@rememberDocumentPicker
            if (it != null) sm.saveImageTo(context, page, it.uri)
        }
        EntryCoverDialog(
            coverData = entry!!,
            artData = entry!!.backgroundUrl?.let { SourceImage(entry!!.source, it) },
            snackbarHostState = sm.snackbarHostState,
            isCustomCover = remember(entry) { sm.hasCustomCover(entry!!) },
            isCoverEditable = entry!!.favorite || entry!!.source == LOCAL_SOURCE_ID,
            onShareClick = { page: EntryCoverPage -> sm.shareImage(context, page) }
                .takeIf { sm.isSharingAvailable },
            onSaveClick = { page ->
                if (sm.savesToPickedDocument) {
                    pageToSave = page
                    saveDocument.launch(sm.documentName(page))
                } else {
                    sm.saveImage(context, page)
                }
            },
            onEditClick = { action: EditCoverAction ->
                when (action) {
                    EditCoverAction.EDIT -> getContent.launch()
                    EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                    EditCoverAction.ADD_TO_LIBRARY -> sm.showAddToLibraryFirst(context)
                }
                Unit
            }.takeIf { sm.isCoverReplaceable(entry!!) },
            onDismissRequest = onDismissRequest,
        )
    } else {
        LoadingScreen(Modifier.systemBarsPadding())
    }
}
