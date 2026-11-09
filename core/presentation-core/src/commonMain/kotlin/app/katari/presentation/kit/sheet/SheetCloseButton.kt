package app.katari.presentation.kit.sheet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.Close
import app.katari.presentation.kit.button.IconButton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Closes the side sheet around the content, sliding it away as a click beside it does; null in a bottom sheet, which a
 * phone closes by a drag or back, in a popover, which closes like a menu, and while the sheet keeps itself open.
 */
internal val LocalSheetClose = compositionLocalOf<(() -> Unit)?> { null }

/**
 * The close button that ends a side sheet's title row, so a mouse has something in the sheet to close it with; nothing
 * where [LocalSheetClose] is null.
 */
@Composable
internal fun SheetCloseButton() {
    val close = LocalSheetClose.current ?: return
    IconButton(
        icon = MaterialSymbols.Rounded.Close,
        contentDescription = stringResource(MR.strings.action_close),
        onClick = close,
    )
}
