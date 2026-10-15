package eu.kanade.presentation.browse.components

import android.content.Context
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.entry.components.preview.PreviewContent
import eu.kanade.presentation.entry.components.preview.PreviewSizeUi
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.entry.interactions.media.EntryPreviewOpenTargetResult
import mihon.entry.interactions.media.EntryPreviewSize
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.progress.LoadingIndicator
import mihon.presentation.kit.sheet.Sheet
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication

@Composable
fun BrowseEntryPreviewSheet(
    entryId: Long,
    onLibraryAction: (Entry) -> Unit,
    onMergeAction: (Entry) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { context.appGraph.profileManager }
    val entryOpenFeature = remember { context.appGraph.entryOpenFeature }
    val activeProfile by profileManager.activeProfile.collectAsStateWithLifecycle()
    var previousProfileId by remember(entryId) { mutableStateOf(activeProfile?.id) }
    var hasRequestedPreview by rememberSaveable(entryId) { mutableStateOf(false) }
    val viewModel = assistedMetroViewModel<EntryViewModel, EntryViewModel.Factory>(
        key = "browse-entry-preview-$entryId",
    ) {
        create(context = context, entryId = entryId, isFromSource = true, bypassMerge = false)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val previewConfig by viewModel.previewConfig.collectAsStateWithLifecycle()
    val previewState by viewModel.previewState.collectAsStateWithLifecycle()

    LaunchedEffect(activeProfile?.id) {
        val currentProfileId = activeProfile?.id
        val lastProfileId = previousProfileId
        previousProfileId = currentProfileId

        if (currentProfileId != null && lastProfileId != null && currentProfileId != lastProfileId) {
            viewModel.setPreviewExpanded(false)
            onDismissRequest()
        }
    }

    LaunchedEffect(state, hasRequestedPreview, previewState.chapterId) {
        if (!hasRequestedPreview && state is EntryViewModel.State.Success && previewState.chapterId == null) {
            hasRequestedPreview = true
            viewModel.setPreviewExpanded(true)
        }
    }

    Sheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxSize(),
        enableImplicitDismiss = false,
    ) {
        when (val currentState = state) {
            EntryViewModel.State.Loading,
            EntryViewModel.State.Error,
            -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
            is EntryViewModel.State.Success -> {
                BrowseEntryPreviewDialogContent(
                    title = currentState.entry.displayTitle,
                    state = currentState,
                    previewState = previewState,
                    previewSize = previewConfig.size.toPreviewSizeUi(),
                    snackbarHostState = viewModel.snackbarHostState,
                    onDismissRequest = onDismissRequest,
                    onLibraryAction = onLibraryAction,
                    onMergeAction = onMergeAction,
                    onOpenEntry = {
                        onDismissRequest()
                        onOpenEntry(currentState.entry.id)
                    },
                    onRetry = viewModel::retryPreview,
                    onPageLoad = viewModel::loadPreviewPage,
                    onPageClick = (
                        { _: Long, pageIndex: Int ->
                            val target = viewModel.previewOpenTarget(pageIndex)
                            if (target is EntryPreviewOpenTargetResult.Available) {
                                scope.launch {
                                    val chapter = currentState.chapters.firstOrNull {
                                        it.chapter.id == target.childId
                                    }?.chapter ?: return@launch
                                    openChapter(
                                        context,
                                        entryOpenFeature,
                                        currentState.entry,
                                        chapter,
                                        target.pageIndex,
                                    )
                                }
                            }
                        }
                        ).takeIf {
                        viewModel.isPreviewOpenApplicable(currentState.entry.type)
                    },
                )
            }
        }
    }
}

@Composable
private fun BrowseEntryPreviewDialogContent(
    title: String,
    state: EntryViewModel.State.Success,
    previewState: EntryViewModel.EntryPreviewState,
    previewSize: PreviewSizeUi,
    snackbarHostState: SnackbarHostState,
    onDismissRequest: () -> Unit,
    onLibraryAction: (Entry) -> Unit,
    onMergeAction: (Entry) -> Unit,
    onOpenEntry: () -> Unit,
    onRetry: () -> Unit,
    onPageLoad: (Int) -> Unit,
    onPageClick: ((Long, Int) -> Unit)?,
) {
    val displayedPreviewState = if (
        previewState.chapterId == null && previewState.pages.isEmpty() && state.isRefreshingData
    ) {
        previewState.copy(
            isLoading = true,
            error = null,
        )
    } else {
        previewState
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        container = SurfaceContainer.High,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppBar(
                titleContent = {
                    BrowseEntryPreviewTitle(title = title)
                },
                navigateUp = onDismissRequest,
            )
        },
        bottomBar = {
            BrowseEntryPreviewBottomBar(
                favorite = state.entry.favorite,
                onOpenEntry = onOpenEntry,
                onLibraryAction = { onLibraryAction(state.entry) },
                onMergeAction = { onMergeAction(state.entry) },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .systemBarsPadding()
                .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        ) {
            PreviewContent(
                state = displayedPreviewState,
                size = previewSize,
                onRetry = onRetry,
                onPageLoad = onPageLoad,
                onPageClick = onPageClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState()),
                centerStates = true,
                loadingContent = {
                    PreviewLoadingContent()
                },
            )
        }
    }
}

@Composable
private fun BoxScope.PreviewLoadingContent() {
    Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        LoadingIndicator()
        Text(
            text = stringResource(MR.strings.transition_pages_loading),
            role = TextRole.Body,
        )
    }
}

@Composable
private fun BrowseEntryPreviewTitle(title: String) {
    val context = LocalContext.current

    Text(
        text = title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoIndication(
                onLongClick = {
                    context.copyToClipboard(title, title)
                },
                onClick = {},
            )
            .basicMarquee(
                repeatDelayMillis = 2_000,
            ),
    )
}

private suspend fun openChapter(
    context: Context,
    entryOpenFeature: EntryOpenFeature,
    entry: Entry,
    chapter: EntryChapter,
    pageIndex: Int? = null,
) {
    entryOpenFeature.open(
        context = context,
        entry = entry,
        chapter = chapter,
        options = EntryOpenOptions(pageIndex = pageIndex),
    )
}

private fun EntryPreviewSize.toPreviewSizeUi(): PreviewSizeUi {
    return when (this) {
        EntryPreviewSize.SMALL -> PreviewSizeUi.SMALL
        EntryPreviewSize.MEDIUM -> PreviewSizeUi.MEDIUM
        EntryPreviewSize.LARGE -> PreviewSizeUi.LARGE
        EntryPreviewSize.EXTRA_LARGE -> PreviewSizeUi.EXTRA_LARGE
    }
}
