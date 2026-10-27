package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.documentSpanStyle
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.theme.KatariTheme

/**
 * One font of a set where only one can be chosen, its [title] drawn in the font itself so the list previews each face.
 * [font] is content, such as a face the reader offers or a file the user imported; null draws the title in the app's
 * own face, for a choice that keeps whatever the content asks for. [trailing] holds an action on the font, such as
 * deleting an imported one.
 */
@Composable
fun FontChoiceRow(
    title: String,
    font: FontFamily?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    RowFrame(
        modifier = modifier.dialogSelectionFill(selected),
        enabled = true,
        role = Role.RadioButton,
        onClick = onClick,
        onLongClick = null,
    ) {
        TrailingControlsLayout(
            leading = { RadioButton(selected, onClick = null) },
            words = {
                val text = buildAnnotatedString {
                    withStyle(documentSpanStyle(fontFamily = font)) { append(title) }
                }
                Text(text, role = LocalRowDensity.current.titleRole)
            },
            trailing = { if (trailing != null) RowTrailing(trailing) },
            leadingGap = KatariTheme.spacing.l,
            trailingGap = KatariTheme.spacing.l,
        )
    }
}
