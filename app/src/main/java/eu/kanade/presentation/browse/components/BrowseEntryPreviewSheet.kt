package eu.kanade.presentation.browse.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.entry.components.PreviewContent
import eu.kanade.presentation.entry.components.PreviewSizeUi
import eu.kanade.tachiyomi.ui.entry.EntryScreenModel
import eu.kanade.tachiyomi.util.system.copyToClipboard
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryOpenFeature
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryPreviewOpenTargetResult
import mihon.entry.interactions.EntryPreviewSize
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun BrowseEntryPreviewSheet(
    entryId: Long,
    onLibraryAction: (Entry) -> Unit,
    onMergeAction: (Entry) -> Unit,
    onOpenEntry: (Long) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val profileManager = remember { Injekt.get<ProfileManager>() }
    val entryOpenFeature = remember { Injekt.get<EntryOpenFeature>() }
    val activeProfile by profileManager.activeProfile.collectAsStateWithLifecycle()
    var previousProfileId by remember(entryId) { mutableStateOf(activeProfile?.id) }
    var hasRequestedPreview by rememberSaveable(entryId) { mutableStateOf(false) }
    val screenModel = object : Screen {
        override val key: ScreenKey = "browse-entry-preview-screen-$entryId"

        @Composable
        override fun Content() {
            error("Not used")
        }
    }.rememberScreenModel(tag = entryId.toString()) {
        EntryScreenModel(
            context = context,
            lifecycle = lifecycleOwner.lifecycle,
            entryId = entryId,
            isFromSource = true,
        )
    }

    val state by screenModel.state.collectAsStateWithLifecycle()
    val previewConfig by screenModel.previewConfig.collectAsStateWithLifecycle()
    val previewState by screenModel.previewState.collectAsStateWithLifecycle()

    LaunchedEffect(activeProfile?.id) {
        val currentProfileId = activeProfile?.id
        val lastProfileId = previousProfileId
        previousProfileId = currentProfileId

        if (currentProfileId != null && lastProfileId != null && currentProfileId != lastProfileId) {
            screenModel.setPreviewExpanded(false)
            onDismissRequest()
        }
    }

    LaunchedEffect(state, hasRequestedPreview, previewState.chapterId) {
        if (!hasRequestedPreview && state is EntryScreenModel.State.Success && previewState.chapterId == null) {
            hasRequestedPreview = true
            screenModel.setPreviewExpanded(true)
        }
    }

    AdaptiveSheet(
        onDismissRequest = onDismissRequest,
        modifier = Modifier.fillMaxSize(),
        enableImplicitDismiss = false,
    ) {
        when (val currentState = state) {
            EntryScreenModel.State.Loading,
            EntryScreenModel.State.Error,
            -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is EntryScreenModel.State.Success -> {
                BrowseEntryPreviewDialogContent(
                    title = currentState.entry.displayTitle,
                    state = currentState,
                    previewState = previewState,
                    previewSize = previewConfig.size.toPreviewSizeUi(),
                    snackbarHostState = screenModel.snackbarHostState,
                    onDismissRequest = onDismissRequest,
                    onLibraryAction = onLibraryAction,
                    onMergeAction = onMergeAction,
                    onOpenEntry = {
                        onDismissRequest()
                        onOpenEntry(currentState.entry.id)
                    },
                    onRetry = screenModel::retryPreview,
                    onPageLoad = screenModel::loadPreviewPage,
                    onPageClick = (
                        { _: Long, pageIndex: Int ->
                            val target = screenModel.previewOpenTarget(pageIndex)
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
                        screenModel.isPreviewOpenApplicable(currentState.entry.type)
                    },
                )
            }
        }
    }
}

@Composable
private fun BrowseEntryPreviewDialogContent(
    title: String,
    state: EntryScreenModel.State.Success,
    previewState: EntryScreenModel.EntryPreviewState,
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
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
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
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
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(MR.strings.transition_pages_loading),
            style = MaterialTheme.typography.bodyMedium,
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

@Composable
private fun BrowseEntryPreviewBottomBar(
    favorite: Boolean,
    onOpenEntry: () -> Unit,
    onLibraryAction: () -> Unit,
    onMergeAction: () -> Unit,
) {
    val spacing = MaterialTheme.padding.small
    val openLabel = stringResource(MR.strings.action_open)
    val libraryLabel = stringResource(if (favorite) MR.strings.remove_from_library else MR.strings.add_to_library)
    val mergeLabel = stringResource(MR.strings.action_add_to_merge)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        HorizontalDivider()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = MaterialTheme.padding.medium, vertical = MaterialTheme.padding.small),
        ) {
            val useCompactLayout = maxWidth < 480.dp

            if (useCompactLayout) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    FilledTonalButton(
                        onClick = onOpenEntry,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        PreviewBottomBarActionContent(
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            label = openLabel,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        Button(
                            onClick = onLibraryAction,
                            modifier = Modifier.weight(1f),
                        ) {
                            PreviewBottomBarActionContent(
                                icon = if (favorite) Icons.Outlined.Delete else Icons.Outlined.FavoriteBorder,
                                label = libraryLabel,
                            )
                        }
                        FilledTonalButton(
                            onClick = onMergeAction,
                            modifier = Modifier.weight(1f),
                        ) {
                            PreviewBottomBarActionContent(
                                icon = Icons.AutoMirrored.Outlined.CallSplit,
                                label = mergeLabel,
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    FilledTonalButton(
                        onClick = onOpenEntry,
                        modifier = Modifier.weight(1f),
                    ) {
                        PreviewBottomBarActionContent(
                            icon = Icons.AutoMirrored.Outlined.OpenInNew,
                            label = openLabel,
                        )
                    }
                    Button(
                        onClick = onLibraryAction,
                        modifier = Modifier.weight(1f),
                    ) {
                        PreviewBottomBarActionContent(
                            icon = if (favorite) Icons.Outlined.Delete else Icons.Outlined.FavoriteBorder,
                            label = libraryLabel,
                        )
                    }
                    FilledTonalButton(
                        onClick = onMergeAction,
                        modifier = Modifier.weight(1f),
                    ) {
                        PreviewBottomBarActionContent(
                            icon = Icons.AutoMirrored.Outlined.CallSplit,
                            label = mergeLabel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBottomBarActionContent(icon: ImageVector, label: String) {
    Icon(
        imageVector = icon,
        contentDescription = null,
    )
    Spacer(modifier = Modifier.width(MaterialTheme.padding.small))
    Text(text = label)
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
