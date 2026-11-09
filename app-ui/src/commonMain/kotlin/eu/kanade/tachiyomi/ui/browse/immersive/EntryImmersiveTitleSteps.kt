package eu.kanade.tachiyomi.ui.browse.immersive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.entry.viewer.chrome.ui.pointer.mediaControl
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.KeyboardArrowDown
import app.katari.icons.materialsymbols.rounded.KeyboardArrowUp
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.button.IconButtonSize
import app.katari.presentation.kit.button.IconButtonStyle
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** The previous and next title buttons: [hasPrevious] and [hasNext] say whether there is a title that way. */
internal class ImmersiveTitleSteps(
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
)

/** Round buttons to the previous and next title, whose tooltips name ↑ and ↓, the keys that do the same. */
@Composable
internal fun ImmersiveTitleStepButtons(steps: ImmersiveTitleSteps, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
        IconButton(
            icon = MaterialSymbols.Rounded.KeyboardArrowUp,
            contentDescription = stringResource(MR.strings.browse_immersive_previous),
            onClick = steps.onPrevious,
            modifier = Modifier.mediaControl(),
            style = IconButtonStyle.Tonal,
            size = IconButtonSize.Large,
            enabled = steps.hasPrevious,
            shortcut = previousTitleShortcut(),
        )
        IconButton(
            icon = MaterialSymbols.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(MR.strings.browse_immersive_next),
            onClick = steps.onNext,
            modifier = Modifier.mediaControl(),
            style = IconButtonStyle.Tonal,
            size = IconButtonSize.Large,
            enabled = steps.hasNext,
            shortcut = nextTitleShortcut(),
        )
    }
}
