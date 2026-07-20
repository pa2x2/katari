package tachiyomi.presentation.core.components.reader

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
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
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    warningColor: Color = MaterialTheme.colorScheme.error,
    outlineColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Column(
                modifier = modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                model.topChild?.let { child ->
                    TransitionChild(
                        header = model.topLabel,
                        child = child,
                        prominent = false,
                    )
                } ?: NoChildNotification(
                    text = model.fallbackLabel,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    outlineColor = outlineColor,
                )

                model.bottomChild?.let { child ->
                    if (model.missingChildCount > 0) {
                        ChildGapWarning(
                            gapCount = model.missingChildCount,
                            contentColor = contentColor,
                            warningColor = warningColor,
                            outlineColor = outlineColor,
                        )
                    } else {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.32f),
                            color = outlineColor,
                        )
                    }
                    TransitionChild(
                        header = model.bottomLabel,
                        child = child,
                        prominent = true,
                    )
                } ?: NoChildNotification(
                    text = model.fallbackLabel,
                    contentColor = contentColor,
                    accentColor = accentColor,
                    outlineColor = outlineColor,
                )
            }
        }
    }
}

@Composable
private fun NoChildNotification(
    text: String,
    contentColor: Color,
    accentColor: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
        ),
        border = BorderStroke(1.dp, outlineColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                tint = accentColor,
                contentDescription = null,
            )
            Text(text = text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChildGapWarning(
    gapCount: Int,
    contentColor: Color,
    warningColor: Color,
    outlineColor: Color,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
        ),
        border = BorderStroke(1.dp, outlineColor),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                tint = warningColor,
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
    prominent: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = header,
            modifier = Modifier.secondaryItemAlpha().padding(bottom = 2.dp),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildAnnotatedString {
                if (child.availableOffline) {
                    appendInlineContent(AVAILABLE_OFFLINE_ICON_ID)
                    append(' ')
                }
                append(child.name)
            },
            fontSize = if (prominent) 22.sp else 18.sp,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            inlineContent = mapOf(
                AVAILABLE_OFFLINE_ICON_ID to InlineTextContent(
                    Placeholder(
                        width = if (prominent) 24.sp else 20.sp,
                        height = if (prominent) 24.sp else 20.sp,
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
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val AVAILABLE_OFFLINE_ICON_ID = "available_offline"
