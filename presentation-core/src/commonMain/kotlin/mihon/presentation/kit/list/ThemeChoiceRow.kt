package mihon.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.domain.ui.model.AppTheme
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.roundedfilled.CheckCircle
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The app themes to pick from, each drawn as a small screen in its own colours. [themes] are the ones this device
 * offers; [amoled] draws dark variants on pure black, as they will look once chosen.
 */
@Composable
fun ThemeChoiceRow(
    themes: List<AppTheme>,
    selected: AppTheme,
    amoled: Boolean,
    onSelect: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.padding(vertical = LocalRowDensity.current.verticalPadding),
        contentPadding = PaddingValues(horizontal = LocalRowInset.current),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
    ) {
        items(items = themes, key = { it.name }) { theme ->
            Column(
                modifier = Modifier.width(ThemePreviewWidth),
                verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                KatariTheme(appTheme = theme, amoled = amoled) {
                    ThemePreview(selected = theme == selected, onClick = { onSelect(theme) })
                }
                Text(
                    text = theme.titleRes?.let { stringResource(it) }.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    role = TextRole.Body,
                    tone = ContentTone.Secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    minLines = 2,
                )
            }
        }
    }
}

/** A miniature app screen (app bar, a cover and a bottom bar) drawn in the current theme's colours. */
@Composable
private fun ThemePreview(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KatariTheme.colors
    val shapes = KatariTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PREVIEW_ASPECT_RATIO)
            .border(
                width = PreviewFrameWidth,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = RoundedCornerShape(PreviewOuterCorner),
            )
            .padding(PreviewFrameWidth)
            .clip(RoundedCornerShape(PreviewInnerCorner))
            .background(colors.background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PreviewAppBarHeight)
                .padding(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8f)
                    .weight(0.7f)
                    .padding(end = KatariTheme.spacing.xs)
                    .background(color = colors.onSurface, shape = shapes.small),
            )
            Box(
                modifier = Modifier.weight(0.3f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (selected) {
                    Icon(
                        imageVector = MaterialSymbols.RoundedFilled.CheckCircle,
                        contentDescription = stringResource(MR.strings.selected),
                        tone = ContentTone.Primary,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(start = KatariTheme.spacing.s, top = KatariTheme.spacing.xxs)
                .background(color = colors.outlineVariant, shape = shapes.small)
                .fillMaxWidth(0.5f)
                .aspectRatio(COVER_ASPECT_RATIO),
        ) {
            Row(
                modifier = Modifier
                    .padding(KatariTheme.spacing.xs)
                    .size(width = PreviewBadgeWidth, height = PreviewBadgeHeight)
                    .clip(shapes.extraSmall),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(colors.tertiary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(colors.secondary),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                modifier = Modifier
                    .background(colors.surfaceContainer)
                    .height(PreviewBottomBarHeight)
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(PreviewBottomBarItem)
                        .background(color = colors.primary, shape = shapes.full),
                )
                Box(
                    modifier = Modifier
                        .padding(start = KatariTheme.spacing.s)
                        .height(PreviewBottomBarItem)
                        .weight(1f)
                        .background(color = colors.onSurfaceVariant, shape = shapes.small),
                )
            }
        }
    }
}

private const val PREVIEW_ASPECT_RATIO = 9f / 16f
private const val COVER_ASPECT_RATIO = 2f / 3f
private val ThemePreviewWidth = 114.dp
private val PreviewFrameWidth = 4.dp
private val PreviewOuterCorner = 17.dp
private val PreviewInnerCorner = 13.dp
private val PreviewAppBarHeight = 40.dp
private val PreviewBadgeWidth = 24.dp
private val PreviewBadgeHeight = 16.dp
private val PreviewBottomBarHeight = 32.dp
private val PreviewBottomBarItem = 17.dp
