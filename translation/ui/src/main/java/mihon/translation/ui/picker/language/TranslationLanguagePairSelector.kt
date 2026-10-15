package mihon.translation.ui.picker.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDropDown
import mihon.icons.materialsymbols.rounded.SwapHoriz
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** A rounded bar naming the source and target languages, each opening its picker, with a swap button between. */
@Composable
fun TranslationLanguagePairSelector(
    source: String,
    target: String,
    canSwap: Boolean,
    onChooseSource: () -> Unit,
    onChooseTarget: () -> Unit,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.extraLarge,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguageButton(value = source, onClick = onChooseSource, modifier = Modifier.weight(1f))
            IconButton(
                icon = MaterialSymbols.Rounded.SwapHoriz,
                contentDescription = stringResource(MR.strings.translation_settings_swap_languages),
                onClick = onSwap,
                enabled = canSwap,
            )
            LanguageButton(value = target, onClick = onChooseTarget, modifier = Modifier.weight(1f))
        }
    }
}

// A text button with a trailing drop-down arrow, whose label shortens rather than pushing the arrow out.
@Composable
private fun LanguageButton(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        container = SurfaceContainer.None,
        shape = KatariTheme.shapes.full,
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = KatariTheme.sizes.touchTarget)
                .padding(horizontal = KatariTheme.spacing.m),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f, fill = false),
                role = TextRole.Label,
                tone = ContentTone.Primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(MaterialSymbols.Rounded.ArrowDropDown, contentDescription = null, tone = ContentTone.Primary)
        }
    }
}
