package eu.kanade.tachiyomi.ui.download

import android.view.LayoutInflater
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.recyclerview.widget.LinearLayoutManager
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.NestedMenuItem
import eu.kanade.presentation.components.TabContent
import eu.kanade.tachiyomi.databinding.DownloadListBinding
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import kotlin.math.roundToInt

/** The download queue as a tab of the queue screen, with its sorting, Cancel all and Pause. */
@Composable
internal fun downloadQueueDownloadsTab(screenModel: DownloadQueueScreenModel): TabContent {
    val downloadList by screenModel.state.collectAsState()
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
                    icon = Icons.AutoMirrored.Outlined.Sort,
                    onClick = { sortExpanded = true },
                ),
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_cancel_all),
                    onClick = screenModel::clearQueue,
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
                    DownloadList(screenModel, downloadList, contentPadding)
                }
                Box(modifier = Modifier.align(Alignment.TopEnd)) {
                    SortMenu(screenModel, expanded = sortExpanded, onDismissRequest = { sortExpanded = false })
                }
                if (downloadList.isNotEmpty()) {
                    PauseButton(
                        screenModel = screenModel,
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
    screenModel: DownloadQueueScreenModel,
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
            screenModel.controllerBinding = DownloadListBinding.inflate(LayoutInflater.from(context))
            screenModel.adapter = DownloadQueueAdapter(screenModel.listener)
            screenModel.controllerBinding.root.adapter = screenModel.adapter
            screenModel.adapter?.isHandleDragEnabled = true
            screenModel.controllerBinding.root.layoutManager = LinearLayoutManager(context)

            ViewCompat.setNestedScrollingEnabled(screenModel.controllerBinding.root, true)

            screenModel.controllerBinding.root
        },
        update = {
            screenModel.controllerBinding.root.updatePadding(left = left, top = top, right = right, bottom = bottom)
            screenModel.adapter?.updateDataSet(downloadList)
        },
    )
}

@Composable
private fun SortMenu(
    screenModel: DownloadQueueScreenModel,
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
                        screenModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, true)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_oldest)) },
                    onClick = {
                        screenModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().dateUpload }, false)
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
                        screenModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().chapterNumber }, false)
                        closeMenu()
                    },
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_desc)) },
                    onClick = {
                        screenModel.reorderQueue({ item -> item.payloadAsDownloadQueueItem().chapterNumber }, true)
                        closeMenu()
                    },
                )
            },
        )
    }
}

@Composable
private fun PauseButton(screenModel: DownloadQueueScreenModel, modifier: Modifier) {
    val isRunning by screenModel.isDownloaderRunning.collectAsState()
    SmallExtendedFloatingActionButton(
        text = { Text(text = stringResource(if (isRunning) MR.strings.action_pause else MR.strings.action_resume)) },
        icon = {
            Icon(imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
        },
        onClick = { if (isRunning) screenModel.pauseDownloads() else screenModel.startDownloads() },
        modifier = modifier,
    )
}
