package mihon.presentation.kit.content

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.model.RichTextState
import mihon.presentation.kit.theme.KatariTheme
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
    LinkColorEffect(state)
    ProvideTextRole(role) {
        MaterialRichText(state = state, modifier = modifier)
    }
}

/** Draws [state]'s links in the primary colour, following the theme as it changes. */
@Composable
internal fun LinkColorEffect(state: RichTextState) {
    val linkColor = KatariTheme.colors.primary
    LaunchedEffect(state, linkColor) {
        state.config.linkColor = linkColor
    }
}
