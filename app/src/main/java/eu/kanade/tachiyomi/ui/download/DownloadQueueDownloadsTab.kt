package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.Sort
import mihon.icons.materialsymbols.roundedfilled.Pause
import mihon.icons.materialsymbols.roundedfilled.PlayArrow
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
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
                            .padding(16.dp),
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
            text = { Text(text = stringResource(MR.strings.action_order_by_upload_date)) },
            children = { closeMenu ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_newest)) },
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, true)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_oldest)) },
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, false)
                        closeMenu()
                    },
                )
            },
        )
        NestedMenuItem(
            text = { Text(text = stringResource(MR.strings.action_order_by_item_number)) },
            children = { closeMenu ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_asc)) },
                    onClick = {
                        viewModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().chapterNumber }, false)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_desc)) },
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
    SmallExtendedFloatingActionButton(
        text = { Text(text = stringResource(if (isRunning) MR.strings.action_pause else MR.strings.action_resume)) },
        icon = {
            Icon(
                imageVector = if (isRunning) {
                    MaterialSymbols.RoundedFilled.Pause
                } else {
                    MaterialSymbols.RoundedFilled.PlayArrow
                },
                contentDescription = null,
            )
        },
        onClick = { if (isRunning) viewModel.pauseDownloads() else viewModel.startDownloads() },
        modifier = modifier,
    )
}
