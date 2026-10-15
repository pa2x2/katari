package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.menu.NestedMenuItem
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/** The download queue as a tab of the queue screen, with its sorting, Cancel all and Pause. */
@Composable
internal fun downloadQueueDownloadsTab(viewModel: DownloadQueueViewModel): TabContent {
    val downloadList by viewModel.state.collectAsStateWithLifecycle()
    var sortExpanded by remember { mutableStateOf(false) }
    return TabContent(
        titleRes = MR.strings.label_downloads_tab,
        badgeNumber = downloadList.sumOf { it.subItems.size }.takeIf { it > 0 },
        actions = if (downloadList.isEmpty()) {
            emptyList()
        } else {
            listOf(
                AppBar.Action(
                    title = stringResource(MR.strings.action_sort),
                    icon = MaterialSymbols.AutoMirroredRounded.Sort,
                    onClick = { sortExpanded = true },
                ),
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_cancel_all),
                    onClick = viewModel::clearQueue,
                ),
            )
        },
        content = { contentPadding, _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (downloadList.isEmpty()) {
                    EmptyScreen(
                        stringRes = MR.strings.information_no_downloads,
                        modifier = Modifier.padding(contentPadding),
                    )
                } else {
                    DownloadList(viewModel, downloadList, contentPadding)
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    SortMenu(viewModel, expanded = sortExpanded, onDismissRequest = { sortExpanded = false })
                }
                if (downloadList.isNotEmpty()) {
                    PauseButton(
                        viewModel = viewModel,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(contentPadding)
                            .padding(KatariTheme.spacing.l),
                    )
                }
            }
        },
    )
}

@Composable
private fun DownloadList(
    viewModel: DownloadQueueViewModel,
    downloadList: List<DownloadQueueHeaderItem>,
    contentPadding: PaddingValues,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val left = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx().roundToInt() }
    val top = with(density) { contentPadding.calculateTopPadding().toPx().roundToInt() }
    val right = with(density) { contentPadding.calculateRightPadding(layoutDirection).toPx().roundToInt() }
    val bottom = with(density) { contentPadding.calculateBottomPadding().toPx().roundToInt() }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            viewModel.controllerBinding = DownloadListBinding.inflate(LayoutInflater.from(context))
            viewModel.adapter = DownloadQueueAdapter(viewModel.listener)
            viewModel.controllerBinding.root.adapter = viewModel.adapter
            viewModel.adapter?.isHandleDragEnabled = true
            viewModel.controllerBinding.root.layoutManager = LinearLayoutManager(context)

            ViewCompat.setNestedScrollingEnabled(viewModel.controllerBinding.root, true)

            viewModel.controllerBinding.root
        },
        update = {
            viewModel.controllerBinding.root.updatePadding(left = left, top = top, right = right, bottom = bottom)
            viewModel.adapter?.updateDataSet(downloadList)
        },
    )
}

@Composable
private fun SortMenu(
    viewModel: DownloadQueueViewModel,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest) {
        NestedMenuItem(
            label = stringResource(MR.strings.action_order_by_upload_date),
            children = { closeMenu ->
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_newest),
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, true)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_oldest),
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, false)
                        closeMenu()
                    },
                )
            },
        )
        NestedMenuItem(
            label = stringResource(MR.strings.action_order_by_item_number),
            children = { closeMenu ->
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_asc),
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().chapterNumber }, false)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    label = stringResource(MR.strings.action_desc),
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().chapterNumber }, true)
                        closeMenu()
                    },
                )
            },
        )
    }
}

@Composable
private fun PauseButton(viewModel: DownloadQueueViewModel, modifier: Modifier) {
    val isRunning by viewModel.isDownloaderRunning.collectAsStateWithLifecycle()
    FloatingActionButton(
        label = stringResource(if (isRunning) MR.strings.action_pause else MR.strings.action_resume),
        icon = if (isRunning) MaterialSymbols.RoundedFilled.Pause else MaterialSymbols.RoundedFilled.PlayArrow,
        onClick = { if (isRunning) viewModel.pauseDownloads() else viewModel.startDownloads() },
        modifier = modifier,
    )
}
