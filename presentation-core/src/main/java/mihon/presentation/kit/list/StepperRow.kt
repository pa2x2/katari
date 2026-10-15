package mihon.presentation.kit.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.Stepper
import mihon.presentation.kit.theme.KatariTheme

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
) {
    val density = LocalRowDensity.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = LocalRowInset.current, vertical = density.verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xl),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), role = density.titleRole)
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
