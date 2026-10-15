package mihon.presentation.kit.field

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import mihon.presentation.kit.content.ProvideTextRole
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** A frameless editor for formatted text, such as an entry's notes; [state] owns the text and its formatting. */
@Composable
fun RichTextField(
    state: RichTextState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    ProvideTextRole(TextRole.ItemTitle) {
        RichTextEditor(
            state = state,
            modifier = modifier,
            placeholder = { Text(placeholder) },
            colors = RichTextEditorDefaults.richTextEditorColors(
                containerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            contentPadding = PaddingValues(horizontal = KatariTheme.spacing.m),
        )
    }
}
