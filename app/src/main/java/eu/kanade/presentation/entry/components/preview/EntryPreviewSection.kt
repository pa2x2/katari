package eu.kanade.presentation.entry.components.preview

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.entry.preview.EntryPreviewState
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import mihon.icons.materialsymbols.roundedfilled.Image
import mihon.icons.materialsymbols.roundedfilled.Warning
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The collapsible "Preview" section of an entry's details: a header that toggles a grid of the first pages. */
@Composable
fun SharedEntryPreviewSection(
    state: EntryPreviewState,
    size: PreviewSizeUi,
    onExpandedChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onPageLoad: (Int) -> Unit,
    onPageClick: ((Long, Int) -> Unit)?,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = KatariTheme.spacing.contentInset)
            .padding(bottom = KatariTheme.spacing.m)
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = KatariTheme.sizes.touchTarget)
                .clickable { onExpandedChange(!state.isExpanded) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(MR.strings.manga_preview_section_title),
                role = TextRole.SectionTitle,
            )
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.animated_caret_flip)
            Icon(
                painter = rememberAnimatedVectorPainter(image, !state.isExpanded),
                contentDescription = stringResource(
                    if (state.isExpanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                ),
            )
        }

        if (state.isExpanded) {
            PreviewContent(
                state = state,
                size = size,
                onRetry = onRetry,
                onPageLoad = onPageLoad,
                onPageClick = onPageClick,
            )
        }
    }
}

/** The preview pages, or what stands in for them while they load, fail or turn out empty. */
@Composable
fun PreviewContent(
    state: EntryPreviewState,
    size: PreviewSizeUi,
    onRetry: () -> Unit,
    onPageLoad: (Int) -> Unit,
    onPageClick: ((Long, Int) -> Unit)?,
    modifier: Modifier = Modifier,
    centerStates: Boolean = false,
    loadingContent: @Composable BoxScope.() -> Unit = {
        PreviewMessage(
            icon = MaterialSymbols.RoundedFilled.Image,
            text = stringResource(MR.strings.transition_pages_loading),
            modifier = Modifier.align(Alignment.Center),
        )
    },
) {
    Box(modifier = modifier) {
        val stateModifier = if (centerStates) Modifier.align(Alignment.Center) else Modifier
        when {
            state.isLoading && state.pages.isEmpty() -> {
                loadingContent()
            }
            state.error != null -> {
                PreviewError(
                    message = state.error.message ?: stringResource(MR.strings.unknown_error),
                    onRetry = onRetry,
                    modifier = stateModifier,
                )
            }
            state.pages.isEmpty() -> {
                PreviewMessage(
                    icon = MaterialSymbols.RoundedFilled.Warning,
                    text = stringResource(MR.strings.manga_preview_empty),
                    modifier = stateModifier,
                )
            }
            else -> {
                PreviewGrid(
                    pages = state.pages,
                    size = size,
                    chapterId = state.chapterId,
                    onPageLoad = onPageLoad,
                    onPageClick = onPageClick,
                )
            }
        }
    }
}

@Composable
private fun PreviewError(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PreviewMessage(
            icon = MaterialSymbols.RoundedFilled.Warning,
            text = message,
            modifier = Modifier.weight(1f),
        )
        Button(
            label = stringResource(MR.strings.action_retry),
            onClick = onRetry,
            style = ButtonStyle.Text,
            icon = MaterialSymbols.Rounded.Refresh,
        )
    }
}

@Composable
private fun PreviewMessage(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Text(text = text, role = TextRole.Body)
    }
}
