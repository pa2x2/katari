package eu.kanade.presentation.entry.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import eu.kanade.presentation.entry.EditCoverAction
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Delete
import mihon.icons.materialsymbols.rounded.Edit
import mihon.icons.materialsymbols.rounded.Save
import mihon.icons.materialsymbols.rounded.Share
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.dialog.FullScreenDialog
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.PageDots
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A page of the cover viewer. */
enum class EntryCoverPage { Cover, Art }

/**
 * An entry's cover full screen, zoomable. With [artData], the entry's background art is a second page to swipe to;
 * Share and Save act on the page shown, and Edit, which edits the cover, shows only on the cover's page. Without
 * [onShareClick] Share is hidden.
 */
@Composable
fun EntryCoverDialog(
    coverData: Any?,
    artData: Any?,
    isCustomCover: Boolean,
    /** False while a non-local entry is outside the library, which its cover can't be edited from. */
    isCoverEditable: Boolean,
    snackbarHostState: SnackbarHostState,
    onShareClick: ((EntryCoverPage) -> Unit)?,
    onSaveClick: (EntryCoverPage) -> Unit,
    onEditClick: ((EditCoverAction) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val pages = listOfNotNull(EntryCoverPage.Cover, EntryCoverPage.Art.takeIf { artData != null })
    val pagerState = rememberPagerState { pages.size }
    val page = pages[pagerState.currentPage.coerceAtMost(pages.lastIndex)]
    FullScreenDialog(onDismissRequest = onDismissRequest) {
        Scaffold(
            snackbarHostState = snackbarHostState,
            container = SurfaceContainer.None,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KatariTheme.spacing.xs)
                        .navigationBarsPadding(),
                ) {
                    ActionsPill {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_close),
                            onClick = onDismissRequest,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    ActionsPill {
                        AppBarActions(
                            actions = listOfNotNull(
                                onShareClick?.let {
                                    AppBar.Action(
                                        title = stringResource(MR.strings.action_share),
                                        icon = MaterialSymbols.Rounded.Share,
                                        onClick = { it(page) },
                                    )
                                },
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_save),
                                    icon = MaterialSymbols.Rounded.Save,
                                    onClick = { onSaveClick(page) },
                                ),
                            ),
                        )
                        if (onEditClick != null && page == EntryCoverPage.Cover) {
                            Box {
                                var expanded by remember { mutableStateOf(false) }
                                IconButton(
                                    icon = MaterialSymbols.Rounded.Edit,
                                    contentDescription = stringResource(MR.strings.action_edit_cover),
                                    onClick = {
                                        when {
                                            !isCoverEditable -> onEditClick(EditCoverAction.ADD_TO_LIBRARY)
                                            isCustomCover -> expanded = true
                                            else -> onEditClick(EditCoverAction.EDIT)
                                        }
                                    },
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    offset = DpOffset(KatariTheme.spacing.s, KatariTheme.sizes.none),
                                ) {
                                    DropdownMenuItem(
                                        label = stringResource(MR.strings.action_edit),
                                        onClick = {
                                            onEditClick(EditCoverAction.EDIT)
                                            expanded = false
                                        },
                                        icon = MaterialSymbols.Rounded.Edit,
                                    )
                                    DropdownMenuItem(
                                        label = stringResource(MR.strings.action_delete),
                                        onClick = {
                                            onEditClick(EditCoverAction.DELETE)
                                            expanded = false
                                        },
                                        icon = MaterialSymbols.Rounded.Delete,
                                    )
                                }
                            }
                        }
                    }
                }
            },
        ) { contentPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                    CoverViewerImage(
                        data = when (pages[index]) {
                            EntryCoverPage.Cover -> coverData
                            EntryCoverPage.Art -> artData
                        },
                        contentPadding = contentPadding,
                        onClick = onDismissRequest,
                    )
                }
                if (pages.size > 1) {
                    PageDots(
                        count = pages.size,
                        current = pagerState.currentPage,
                        description = stringResource(
                            when (page) {
                                EntryCoverPage.Cover -> MR.strings.cover_viewer_page_cover
                                EntryCoverPage.Art -> MR.strings.cover_viewer_page_art
                            },
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(contentPadding)
                            .padding(bottom = KatariTheme.spacing.l),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionsPill(content: @Composable () -> Unit) {
    OverlayPill {
        Row { content() }
    }
}
