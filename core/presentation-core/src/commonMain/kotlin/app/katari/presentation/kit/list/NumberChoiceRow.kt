package app.katari.presentation.kit.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowLeft
import app.katari.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import app.katari.presentation.kit.button.IconButton
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.field.TextField
import app.katari.presentation.kit.pointer.clickCursor
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A titled grid of numbers where one can be chosen, such as which episode of a season is playing. [numbers] go by rows
 * in the order given, and [selected] is null while none is chosen. A run too long to look through shows the stretch the
 * chosen number is in, with a field named [jumpLabel] under it, such as "Go to episode", that shows a typed number's
 * stretch and chooses it when the typing is confirmed, and buttons beside the field that step through the stretches. [supportingText] says
 * something of the choice under the grid, such as the chosen episode's name. It sits between row group cards, lined up
 * with their edges.
 */
@Composable
fun NumberChoiceRow(
    title: String,
    numbers: List<Int>,
    selected: Int?,
    onSelect: (Int) -> Unit,
    jumpLabel: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    val stretches = numbers.size > LONG_RUN
    // Stepping looks at another stretch without choosing in it; a choice shows its own stretch again.
    var stretch by remember(numbers, selected) { mutableIntStateOf(stretchOf(numbers, selected)) }
    var typed by remember(numbers, selected) { mutableStateOf("") }
    val shown = if (stretches) numbers.drop(stretch * STRETCH).take(STRETCH) else numbers
    val bottomPadding = LocalRowDensity.current.verticalPadding
    Column(modifier) {
        RowGroupHeader(title)
        Column(
            modifier = Modifier
                .padding(
                    start = rowGroupEdgeInset(),
                    end = rowGroupEdgeInset(),
                    bottom = if (supportingText == null) bottomPadding else KatariTheme.spacing.s,
                )
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
        ) {
            shown.chunked(COLUMNS).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
                    line.forEach { number ->
                        NumberCell(
                            number = number,
                            selected = number == selected,
                            onClick = { onSelect(number) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    // A short last line keeps the cells as wide as the lines above.
                    repeat(COLUMNS - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (stretches) {
                Row(
                    modifier = Modifier.padding(top = KatariTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        value = typed,
                        onValueChange = { text ->
                            typed = text.filter(Char::isDigit)
                            val at = numbers.indexOf(typed.toIntOrNull())
                            if (at >= 0) stretch = at / STRETCH
                        },
                        modifier = Modifier.weight(1f),
                        label = jumpLabel,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                typed.toIntOrNull()?.takeIf { it in numbers }?.let(onSelect)
                                // Puts an on-screen keyboard away, so what the choice brings shows.
                                defaultKeyboardAction(ImeAction.Done)
                            },
                        ),
                    )
                    IconButton(
                        icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowLeft,
                        contentDescription = stringResource(MR.strings.action_previous),
                        onClick = { stretch-- },
                        enabled = stretch > 0,
                    )
                    IconButton(
                        icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
                        contentDescription = stringResource(MR.strings.action_next),
                        onClick = { stretch++ },
                        enabled = (stretch + 1) * STRETCH < numbers.size,
                    )
                }
            }
        }
        if (supportingText != null) {
            Text(
                text = supportingText,
                role = TextRole.Supporting,
                modifier = Modifier.padding(
                    start = rowGroupTextInset(),
                    end = rowGroupTextInset(),
                    bottom = bottomPadding,
                ),
            )
        }
    }
}

@Composable
private fun NumberCell(number: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = KatariTheme.shapes.small
    val colors = KatariTheme.colors
    Box(
        modifier = modifier
            .height(CELL_HEIGHT)
            .clip(shape)
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainerHighest)
            .border(
                width = if (selected) KatariTheme.sizes.selectedBorder else KatariTheme.sizes.border,
                color = if (selected) colors.primary else colors.outlineVariant,
                shape = shape,
            )
            .clickCursor()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            role = TextRole.Label,
            tone = if (selected) ContentTone.Inherit else ContentTone.Secondary,
            maxLines = 1,
        )
    }
}

private fun stretchOf(numbers: List<Int>, selected: Int?): Int =
    numbers.indexOf(selected).coerceAtLeast(0) / STRETCH

private const val COLUMNS = 6

// A run longer than this is shown a stretch at a time.
private const val LONG_RUN = 60
private const val STRETCH = 30

private val CELL_HEIGHT = 40.dp
