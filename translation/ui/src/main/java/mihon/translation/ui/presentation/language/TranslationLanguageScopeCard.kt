package mihon.translation.ui.presentation.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.language.api.tag.LanguageTag
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Tells the reader that the content keeps its own [kept] target, with ways to follow the [defaultTarget] again or
 * to make [kept] the default for everything.
 */
@Composable
internal fun TranslationLanguageScopeCard(
    kept: LanguageTag,
    defaultTarget: LanguageTag?,
    onUseDefault: () -> Unit,
    onUseForAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    Card(modifier = modifier.fillMaxWidth(), container = SurfaceContainer.SecondaryContainer) {
        // The text buttons carry their own inset, so the end and bottom padding is smaller.
        Column(modifier = Modifier.padding(start = spacing.l, top = spacing.m, end = spacing.s, bottom = spacing.xs)) {
            Text(
                text = stringResource(MR.strings.translation_language_series_scope, kept.displayName()),
                role = TextRole.Body,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                defaultTarget?.let { target ->
                    Button(
                        label = stringResource(MR.strings.translation_language_use_default, target.displayName()),
                        onClick = onUseDefault,
                        style = ButtonStyle.Text,
                    )
                }
                Button(
                    label = stringResource(MR.strings.translation_language_use_for_all_series),
                    onClick = onUseForAll,
                    style = ButtonStyle.Text,
                )
            }
        }
    }
}
