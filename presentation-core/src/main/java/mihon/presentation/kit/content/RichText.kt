package mihon.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import mihon.presentation.kit.theme.TextRole
import com.mohamedrejeb.richeditor.ui.material3.RichText as MaterialRichText

/** Formatted text the user wrote, such as an entry's notes, shown read-only. */
@OptIn(ExperimentalRichTextApi::class)
@Composable
fun RichText(
    state: RichTextState,
    modifier: Modifier = Modifier,
    role: TextRole = TextRole.Body,
) {
    ProvideTextRole(role) {
        MaterialRichText(state = state, modifier = modifier)
    }
}
