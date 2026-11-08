package mihon.entry.interactions.book.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/** The time as the device's clock shows it, updated as each minute starts. */
@Composable
internal expect fun rememberDeviceTime(): State<String>
