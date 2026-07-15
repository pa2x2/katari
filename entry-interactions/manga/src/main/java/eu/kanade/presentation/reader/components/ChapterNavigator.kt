package eu.kanade.presentation.reader.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.i18n.stringResource

internal enum class ChapterNavigatorType {
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    VERTICAL_LEFT,
    VERTICAL_RIGHT,
    ;

    fun isHorizontal() = this == HORIZONTAL_LTR || this == HORIZONTAL_RTL

    fun toReaderPageNavigatorType(): ReaderPageNavigatorType = when (this) {
        HORIZONTAL_LTR -> ReaderPageNavigatorType.HORIZONTAL_LTR
        HORIZONTAL_RTL -> ReaderPageNavigatorType.HORIZONTAL_RTL
        VERTICAL_LEFT -> ReaderPageNavigatorType.VERTICAL_LEFT
        VERTICAL_RIGHT -> ReaderPageNavigatorType.VERTICAL_RIGHT
    }
}

@Composable
internal fun ChapterNavigator(
    type: ChapterNavigatorType,
    onNextChapter: () -> Unit,
    enabledNext: Boolean,
    onPreviousChapter: () -> Unit,
    enabledPrevious: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    ReaderPageNavigator(
        type = type.toReaderPageNavigatorType(),
        onNextSection = onNextChapter,
        nextSectionEnabled = enabledNext,
        onPreviousSection = onPreviousChapter,
        previousSectionEnabled = enabledPrevious,
        currentPage = currentPage,
        totalPages = totalPages,
        onPageIndexChange = onPageIndexChange,
        previousSectionDescription = stringResource(MR.strings.action_previous_chapter),
        nextSectionDescription = stringResource(MR.strings.action_next_chapter),
        modifier = modifier,
    )
}
