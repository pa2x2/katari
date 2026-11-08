package app.katari.entry.interactions.book.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** In the short time format of the desktop's locale, as Linux desktops leave the 12 or 24 hour choice to it. */
@Composable
internal actual fun rememberDeviceTime(): State<String> {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    val time = remember { mutableStateOf(LocalTime.now().format(formatter)) }
    LaunchedEffect(formatter) {
        while (true) {
            val now = LocalTime.now()
            time.value = now.format(formatter)
            delay(now.until(now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1), ChronoUnit.MILLIS) + 1)
        }
    }
    return time
}
