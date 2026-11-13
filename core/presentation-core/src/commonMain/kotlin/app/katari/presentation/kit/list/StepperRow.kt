package app.katari.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.control.Stepper
import app.katari.presentation.kit.theme.KatariTheme
import tachiyomi.core.common.preference.Preference

/**
 * A number set a step at a time, such as a text size, with the value typed in where the stepper's field allows.
 * [setting] is the setting it sets, whose mark the app shows after the title ([SettingMarks]).
 */
@Composable
fun StepperRow(
    title: String,
    value: Int,
    valueRange: IntRange,
    step: Int,
    valueFormatter: (Int) -> String,
    inputSuffix: String,
    decreaseContentDescription: String,
    increaseContentDescription: String,
    editContentDescription: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    setting: Preference<*>? = null,
) {
    val density = LocalRowDensity.current
    val mark = rememberRowSettingMark(setting, title, density.titleRole, suffix = null)
    Row(
        modifier = modifier
            .then(mark.rowModifier)
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current, vertical = density.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            RowTitle(title, mark.titleSuffix)
        }
        Stepper(
            value = value,
            valueRange = valueRange,
            step = step,
            valueFormatter = valueFormatter,
            inputSuffix = inputSuffix,
            decreaseContentDescription = decreaseContentDescription,
            increaseContentDescription = increaseContentDescription,
            editContentDescription = editContentDescription,
            onValueChange = onValueChange,
        )
    }
}
