package mihon.presentation.kit.surface

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * The first letter of [name] on a tonal tile, standing in for an icon that is missing, such as a source whose
 * extension isn't installed. Size it through [modifier].
 */
@Composable
fun LetterTile(name: String, modifier: Modifier = Modifier) {
    val trimmed = name.trim()
    val letter = if (trimmed.isEmpty()) "?" else String(Character.toChars(trimmed.codePointAt(0))).uppercase()
    Surface(modifier = modifier, container = SurfaceContainer.SecondaryContainer, shape = KatariTheme.shapes.small) {
        Box(contentAlignment = Alignment.Center) {
            Text(letter, role = TextRole.SectionTitle, maxLines = 1)
        }
    }
}
