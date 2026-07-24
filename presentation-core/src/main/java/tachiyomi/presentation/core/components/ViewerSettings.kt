package tachiyomi.presentation.core.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.TabText
import tachiyomi.presentation.core.i18n.stringResource

object ViewerSettingsPaddings {
    val Vertical = 8.dp
}

/**
 * Shared settings dialog for viewer engines.
 *
 * Reset is part of this scaffold so a reader cannot add another tabbed settings
 * surface without also defining how that surface returns to its defaults.
 */
@Composable
fun ViewerSettingsTabbedDialog(
    onDismissRequest: () -> Unit,
    onResetSettings: () -> Unit,
    tabTitles: List<String>,
    modifier: Modifier = Modifier,
    pagerState: PagerState = rememberPagerState { tabTitles.size },
    content: @Composable ColumnScope.(Int) -> Unit,
) {
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = viewerSettingsDialogProperties,
    ) {
        BoxWithConstraints {
            AdaptiveSheet(
                isTabletUi = maxWidth >= VIEWER_TABLET_MIN_WIDTH,
                enableImplicitDismiss = true,
                onDismissRequest = onDismissRequest,
                modifier = modifier.heightIn(max = maxHeight * VIEWER_SETTINGS_MAX_HEIGHT_FRACTION),
            ) {
                Column {
                    Row {
                        PrimaryTabRow(
                            modifier = Modifier.weight(1f),
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            divider = {},
                        ) {
                            tabTitles.fastForEachIndexed { index, title ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { scope.launch { pagerState.scrollToPage(index) } },
                                    text = { TabText(title) },
                                )
                            }
                        }
                        ViewerSettingsResetMenu(onResetSettings)
                    }
                    HorizontalDivider()
                    HorizontalPager(
                        modifier = Modifier.animateContentSize(),
                        state = pagerState,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = ViewerSettingsPaddings.Vertical),
                        ) {
                            content(page)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shared settings sheet for viewer engines that need a full-height transactional
 * surface rather than tabs.
 */
@Composable
fun ViewerSettingsSheet(
    onDismissRequest: () -> Unit,
    onResetSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    )
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(MR.strings.action_settings),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                ViewerSettingsResetMenu(onResetSettings)
            }
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ViewerSettingsResetMenu(onResetSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(MR.strings.label_more),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.action_reset)) },
                onClick = {
                    expanded = false
                    onResetSettings()
                },
            )
        }
    }
}

private val viewerSettingsDialogProperties = DialogProperties(
    usePlatformDefaultWidth = false,
    decorFitsSystemWindows = true,
)
private val VIEWER_TABLET_MIN_WIDTH = 720.dp
private const val VIEWER_SETTINGS_MAX_HEIGHT_FRACTION = 0.75f
