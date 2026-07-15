package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.secondaryItemAlpha

data class ReaderEntryChildTransitionItem(
    val name: String,
    val subtitle: String? = null,
    val availableOffline: Boolean = false,
)

/** Presentation-only transition model shared by built-in entry viewers. */
data class ReaderEntryChildTransitionUiModel(
    val topLabel: String,
    val topChild: ReaderEntryChildTransitionItem?,
    val bottomLabel: String,
    val bottomChild: ReaderEntryChildTransitionItem?,
    val fallbackLabel: String,
    val missingChildCount: Int = 0,
)

/** Format-neutral boundary content for manga chapters, BOOK children, and episodes. */
@Composable
fun ReaderEntryChildTransition(
    model: ReaderEntryChildTransitionUiModel,
    modifier: Modifier = Modifier,
) {
    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
        Column(
            modifier = modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
        ) {
            model.topChild?.let { child ->
                TransitionChild(
                    header = model.topLabel,
                    child = child,
                )
                Spacer(Modifier.height(VerticalSpacerSize))
            } ?: NoChildNotification(
                text = model.fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            model.bottomChild?.let { child ->
                if (model.missingChildCount > 0) {
                    ChildGapWarning(
                        gapCount = model.missingChildCount,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                Spacer(Modifier.height(VerticalSpacerSize))
                TransitionChild(
                    header = model.bottomLabel,
                    child = child,
                )
            } ?: NoChildNotification(
                text = model.fallbackLabel,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun NoChildNotification(
    text: String,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChildGapWarning(
    gapCount: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                tint = MaterialTheme.colorScheme.error,
                contentDescription = null,
            )
            Text(
                text = pluralStringResource(MR.plurals.missing_chapters_warning, count = gapCount, gapCount),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun TransitionChild(
    header: String,
    child: ReaderEntryChildTransitionItem,
) {
    Column {
        Text(
            text = header,
            modifier = Modifier.padding(bottom = 4.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = buildAnnotatedString {
                if (child.availableOffline) {
                    appendInlineContent(AVAILABLE_OFFLINE_ICON_ID)
                    append(' ')
                }
                append(child.name)
            },
            fontSize = 20.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge,
            inlineContent = mapOf(
                AVAILABLE_OFFLINE_ICON_ID to InlineTextContent(
                    Placeholder(
                        width = 22.sp,
                        height = 22.sp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = stringResource(MR.strings.label_downloaded),
                    )
                },
            ),
        )
        child.subtitle?.let {
            Text(
                text = it,
                modifier = Modifier
                    .secondaryItemAlpha()
                    .padding(top = 2.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val VerticalSpacerSize = 24.dp
private const val AVAILABLE_OFFLINE_ICON_ID = "available_offline"
