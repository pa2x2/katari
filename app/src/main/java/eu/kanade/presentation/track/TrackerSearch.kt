package eu.kanade.presentation.track

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import eu.kanade.presentation.entry.components.EntryCover
import eu.kanade.tachiyomi.util.system.openInBrowser
import kotlinx.coroutines.launch
import mihon.entry.interactions.tracking.EntryTrackingSearchCandidate
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.roundedfilled.VisibilityOff
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.button.IconButtonStyle
import mihon.presentation.kit.content.CoverFrame
import mihon.presentation.kit.content.EntryMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.field.PlainTextField
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.SelectableCard
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.runOnEnterKeyPressed

@Composable
fun TrackerSearch(
    state: TextFieldState,
    onDispatchQuery: () -> Unit,
    queryResult: Result<List<EntryTrackingSearchCandidate>>?,
    selected: EntryTrackingSearchCandidate?,
    onSelectedChange: (EntryTrackingSearchCandidate) -> Unit,
    onConfirmSelection: (private: Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    supportsPrivateTracking: Boolean,
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val dispatchQueryAndClearFocus: () -> Unit = {
        onDispatchQuery()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            Column {
                AppBar(
                    titleContent = {
                        PlainTextField(
                            state = state,
                            placeholder = stringResource(MR.strings.action_search_hint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .runOnEnterKeyPressed(action = dispatchQueryAndClearFocus),
                            onSearch = dispatchQueryAndClearFocus,
                        )
                    },
                    navigateUp = onDismissRequest,
                    large = false,
                    actions = {
                        if (state.text.isNotEmpty()) {
                            IconButton(
                                icon = MaterialSymbols.Rounded.Close,
                                contentDescription = stringResource(MR.strings.action_reset),
                                onClick = {
                                    state.clearText()
                                    focusRequester.requestFocus()
                                },
                                tone = ContentTone.Secondary,
                            )
                        }
                    },
                )
                HorizontalDivider()
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = selected != null,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {
                Row(
                    modifier = Modifier
                        .padding(KatariTheme.spacing.s)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        label = stringResource(MR.strings.action_track),
                        onClick = { onConfirmSelection(false) },
                        modifier = Modifier.weight(1f),
                    )
                    if (supportsPrivateTracking) {
                        IconButton(
                            icon = MaterialSymbols.RoundedFilled.VisibilityOff,
                            contentDescription = stringResource(MR.strings.action_toggle_private_on),
                            onClick = { onConfirmSelection(true) },
                            style = IconButtonStyle.Filled,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        if (queryResult == null) {
            LoadingScreen(modifier = Modifier.padding(innerPadding))
        } else {
            val availableTracks = queryResult.getOrNull()
            if (availableTracks != null) {
                if (availableTracks.isEmpty()) {
                    EmptyScreen(
                        modifier = Modifier.padding(innerPadding),
                        stringRes = MR.strings.no_results_found,
                    )
                } else {
                    ScrollbarLazyColumn(
                        contentPadding = innerPadding + PaddingValues(vertical = KatariTheme.spacing.m),
                        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
                    ) {
                        items(
                            items = availableTracks,
                            key = { it.hashCode() },
                        ) {
                            SearchResultItem(
                                trackSearch = it,
                                selected = it == selected,
                                onClick = { onSelectedChange(it) },
                            )
                        }
                    }
                }
            } else {
                EmptyScreen(
                    modifier = Modifier.padding(innerPadding),
                    message = queryResult.exceptionOrNull()?.message
                        ?: stringResource(MR.strings.unknown_error),
                )
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    trackSearch: EntryTrackingSearchCandidate,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard: Clipboard = LocalClipboard.current
    val focusManager = LocalFocusManager.current
    val locale = LocalLocale.current
    val type = trackSearch.publishingType.toLowerCase(locale).capitalize(locale)
    val status = trackSearch.publishingStatus.toLowerCase(locale).capitalize(locale)
    val description = trackSearch.summary.trim()
    var dropDownMenuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    SelectableCard(
        selected = selected,
        onClick = {
            focusManager.clearFocus()
            onClick()
        },
        modifier = Modifier.padding(horizontal = KatariTheme.spacing.contentInset),
        onLongClick = { dropDownMenuExpanded = true },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
            EntryCover.Book(
                data = trackSearch.coverUrl,
                modifier = Modifier.height(EntryMetrics.trackerResultCoverHeight),
                frame = CoverFrame.Thumbnail,
            )
            Column {
                Text(
                    text = trackSearch.title,
                    role = TextRole.SectionTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                SearchResultItemDropDownMenu(
                    expanded = dropDownMenuExpanded,
                    onCollapseMenu = { dropDownMenuExpanded = false },
                    onCopyName = {
                        scope.launch {
                            val clipEntry = ClipData.newPlainText(
                                trackSearch.title,
                                trackSearch.title,
                            ).toClipEntry()
                            clipboard.setClipEntry(clipEntry)
                        }
                    },
                    onOpenInBrowser = {
                        val url = trackSearch.remoteUrl
                        if (url.isNotBlank()) {
                            context.openInBrowser(url)
                        }
                    },
                )
                if (trackSearch.authors.isNotEmpty() || trackSearch.artists.isNotEmpty()) {
                    Text(
                        text = (trackSearch.authors + trackSearch.artists).distinct().joinToString(),
                        role = TextRole.Supporting,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (type.isNotBlank()) {
                    SearchResultItemDetails(
                        title = stringResource(MR.strings.track_type),
                        text = type,
                    )
                }
                if (trackSearch.publicationStartDate.isNotBlank()) {
                    SearchResultItemDetails(
                        title = stringResource(MR.strings.label_started),
                        text = trackSearch.publicationStartDate,
                    )
                }
                if (status.isNotBlank()) {
                    SearchResultItemDetails(
                        title = stringResource(MR.strings.track_status),
                        text = status,
                    )
                }
                if (trackSearch.score != -1.0) {
                    SearchResultItemDetails(
                        title = stringResource(MR.strings.score),
                        text = trackSearch.score.toString(),
                    )
                }
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                role = TextRole.Supporting,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SearchResultItemDropDownMenu(
    expanded: Boolean,
    onCollapseMenu: () -> Unit,
    onCopyName: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onCollapseMenu,
    ) {
        DropdownMenuItem(
            label = stringResource(MR.strings.action_copy_to_clipboard),
            onClick = {
                onCopyName()
                onCollapseMenu()
            },
        )
        DropdownMenuItem(
            label = stringResource(MR.strings.action_open_in_browser),
            onClick = {
                onOpenInBrowser()
            },
        )
    }
}

@Composable
private fun SearchResultItemDetails(
    title: String,
    text: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
        Text(
            text = title,
            role = TextRole.Label,
            maxLines = 1,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun TrackerSearchPreviews(
    @PreviewParameter(TrackerSearchPreviewProvider::class)
    content: @Composable () -> Unit,
) {
    KatariPreviewTheme { content() }
}
