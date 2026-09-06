package tachiyomi.presentation.core.components.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt

enum class ReaderPageNavigatorType {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    ;

    fun isHorizontal() = this == HORIZONTAL_LTR || this == HORIZONTAL_RTL
}

@Composable
fun ReaderPageNavigator(
    type: ReaderPageNavigatorType,
    onNextSection: () -> Unit,
    nextSectionEnabled: Boolean,
    onPreviousSection: () -> Unit,
    previousSectionEnabled: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    onPageIndexChangeFinished: ((Int) -> Unit)? = null,
    showSinglePageLabel: Boolean = false,
    previousSectionDescription: String,
    nextSectionDescription: String,
    modifier: Modifier = Modifier,
) {
    val total = totalPages.coerceAtLeast(1)
    ReaderPositionNavigator(
        type = type,
        onNextSection = onNextSection,
        nextSectionEnabled = nextSectionEnabled,
        onPreviousSection = onPreviousSection,
        previousSectionEnabled = previousSectionEnabled,
        value = currentPage.coerceIn(1, total).toFloat(),
        valueRange = 1f..total.toFloat(),
        steps = (total - 2).coerceAtLeast(0),
        formatValue = { it.roundToInt().toString() },
        endLabel = total.toString(),
        onValueChange = { onPageIndexChange(it.roundToInt() - 1) },
        onValueChangeFinished = { onPageIndexChangeFinished?.invoke(it.roundToInt() - 1) },
        previousSectionDescription = previousSectionDescription,
        nextSectionDescription = nextSectionDescription,
        modifier = modifier,
        showSinglePageLabel = showSinglePageLabel,
    )
}
