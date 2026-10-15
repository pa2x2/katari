package mihon.feature.upcoming.components.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowLeft
import mihon.icons.materialsymbols.automirroredrounded.KeyboardArrowRight
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariPreviewTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.format.DateTimeFormatter
import kotlin.time.Clock

@Composable
fun CalenderHeader(
    yearMonth: YearMonth,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = { getAnimation() },
            label = "Change Month",
        ) { monthYear ->
            Text(text = getTitleText(monthYear), role = TextRole.ScreenTitle)
        }
        Row {
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowLeft,
                contentDescription = stringResource(MR.strings.upcoming_calendar_prev),
                onClick = onPreviousClick,
            )
            IconButton(
                icon = MaterialSymbols.AutoMirroredRounded.KeyboardArrowRight,
                contentDescription = stringResource(MR.strings.upcoming_calendar_next),
                onClick = onNextClick,
            )
        }
    }
}

private const val MONTH_YEAR_CHANGE_ANIMATION_DURATION = 200

private fun AnimatedContentTransitionScope<YearMonth>.getAnimation(): ContentTransform {
    val movingForward = targetState > initialState

    val enterTransition = slideInVertically(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    ) { height -> if (movingForward) height else -height } + fadeIn(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    )
    val exitTransition = slideOutVertically(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    ) { height -> if (movingForward) -height else height } + fadeOut(
        animationSpec = tween(durationMillis = MONTH_YEAR_CHANGE_ANIMATION_DURATION),
    )
    return (enterTransition togetherWith exitTransition)
        .using(SizeTransform(clip = false))
}

@Composable
private fun getTitleText(monthYear: YearMonth): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    return formatter.format(monthYear.toJavaYearMonth())
}

@Preview
@Composable
private fun CalenderHeaderPreview() {
    KatariPreviewTheme {
        CalenderHeader(
            yearMonth = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.yearMonth,
            onNextClick = {},
            onPreviousClick = {},
        )
    }
}
