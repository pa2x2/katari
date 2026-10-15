package mihon.presentation.kit.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import mihon.presentation.kit.theme.KatariSpacing
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/**
 * How tightly rows are packed. Screens use [Comfortable]; sheets set [Compact] for their whole content, so rows never
 * choose it themselves. Density also decides the toggle control: a switch when comfortable, a checkbox when compact.
 */
enum class RowDensity {
    Comfortable,
    Compact,
}

val LocalRowDensity = staticCompositionLocalOf { RowDensity.Comfortable }

/** Side inset of rows and their headers. Dialogs set it to zero because their content area is already inset. */
internal val LocalRowInset = staticCompositionLocalOf { KatariSpacing.rowInset }

@Composable
fun ProvideRowDensity(density: RowDensity, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalRowDensity provides density, content = content)
}

internal val RowDensity.verticalPadding: Dp
    get() = when (this) {
        RowDensity.Comfortable -> KatariTheme.spacing.l
        RowDensity.Compact -> RowCompactVerticalPadding
    }

internal val RowDensity.titleRole: TextRole
    get() = when (this) {
        RowDensity.Comfortable -> TextRole.ItemTitle
        RowDensity.Compact -> TextRole.Body
    }

// Between the s and m steps on purpose: compact rows sit 44dp tall with a checkbox, as sheets always have.
private val RowCompactVerticalPadding = KatariTheme.spacing.s + KatariTheme.spacing.xxs
