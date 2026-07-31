package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun TranslationLanguagePairSelector(
    source: String,
    target: String,
    canSwap: Boolean,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
    sourceModifier: Modifier = Modifier,
    targetModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TranslationLanguageSelector(
            label = stringResource(MR.strings.translation_settings_from),
            value = source,
            onClick = onChooseSource,
            modifier = sourceModifier.weight(1f),
        )
        IconButton(
            onClick = onSwap,
            enabled = canSwap,
        ) {
            Icon(
                imageVector = Icons.Outlined.SwapHoriz,
                contentDescription = stringResource(MR.strings.translation_settings_swap_languages),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TranslationLanguageSelector(
            label = stringResource(MR.strings.translation_settings_to),
            value = target,
            onClick = onChooseTarget,
            modifier = targetModifier.weight(1f),
        )
    }
}

@Composable
private fun TranslationLanguageSelector(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Text(
                text = value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
