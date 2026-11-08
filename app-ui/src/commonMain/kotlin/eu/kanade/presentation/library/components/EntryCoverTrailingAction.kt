package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.button.ResumeButton
import app.katari.presentation.kit.button.ResumeButtonSize
import app.katari.presentation.kit.control.SelectionMark
import dev.icerock.moko.resources.StringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The action at the cover's trailing corner: while selecting it shows whether the item is selected, replacing the
 * resume button so a tap meant to select can't open the entry.
 */
internal fun coverTrailingAction(
    selectionMode: Boolean,
    isSelected: Boolean,
    onClickContinueReading: (() -> Unit)?,
    continueReadingProgress: Float?,
    continueReadingContentDescription: StringResource,
    large: Boolean,
    onCover: Boolean = true,
): (@Composable (Modifier) -> Unit)? {
    if (selectionMode) {
        return { modifier -> SelectionMark(selected = isSelected, modifier = modifier, onMedia = onCover) }
    }
    if (onClickContinueReading == null) return null
    return { modifier ->
        ResumeButton(
            size = if (large) ResumeButtonSize.Large else ResumeButtonSize.Small,
            onClick = onClickContinueReading,
            contentDescription = stringResource(continueReadingContentDescription),
            modifier = modifier,
            progress = continueReadingProgress,
        )
    }
}
