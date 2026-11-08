package app.katari.presentation.kit.field

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.katari.presentation.kit.content.LinkColorEffect
import app.katari.presentation.kit.content.ProvideTextRole
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.shortcut.typingFocus
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/** A frameless editor for formatted text, such as an entry's notes; [state] owns the text and its formatting. */
@Composable
fun RichTextField(
    state: RichTextState,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    LinkColorEffect(state)
    ProvideTextRole(TextRole.ItemTitle) {
        RichTextEditor(
            state = state,
            modifier = modifier.typingFocus(),
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
