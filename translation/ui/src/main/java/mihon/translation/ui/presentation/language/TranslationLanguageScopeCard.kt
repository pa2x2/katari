package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mihon.language.api.tag.LanguageTag
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Tells the reader that the content keeps its own [kept] target, with ways to follow the [defaultTarget] again or
 * to make [kept] the default for everything.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TranslationLanguageScopeCard(
    kept: LanguageTag,
    defaultTarget: LanguageTag?,
    onUseDefault: () -> Unit,
    onUseForAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Text(
                text = stringResource(MR.strings.translation_language_series_scope, kept.displayName()),
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                defaultTarget?.let { target ->
                    TextButton(onClick = onUseDefault) {
                        Text(stringResource(MR.strings.translation_language_use_default, target.displayName()))
                    }
                }
                TextButton(onClick = onUseForAll) {
                    Text(stringResource(MR.strings.translation_language_use_for_all_series))
                }
            }
        }
    }
}
