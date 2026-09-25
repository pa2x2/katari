package eu.kanade.presentation.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.viewer.settings.ViewerSettingBinding
import kotlin.time.Duration.Companion.milliseconds

@Stable
internal class DisplayRefreshHost(
    internal val flashDurationMillis: ViewerSettingBinding<Int>,
    internal val flashColor: ViewerSettingBinding<MangaReaderSettings.FlashColor>,
    internal val flashPageInterval: ViewerSettingBinding<Int>,
) {

    internal var currentDisplayRefresh by mutableStateOf(false)

    // Internal State for Flash
    private var flashInterval = flashPageInterval.state.value.effectiveValue
    private var timesCalled = 0

    fun flash() {
        if (timesCalled % flashInterval == 0) {
            currentDisplayRefresh = true
        }
        timesCalled += 1
    }

    fun setInterval(interval: Int) {
        flashInterval = interval
        timesCalled = 0
    }
}

@Composable
internal fun DisplayRefreshHost(
    hostState: DisplayRefreshHost,
    modifier: Modifier = Modifier,
) {
    val currentDisplayRefresh = hostState.currentDisplayRefresh
    val refreshDuration by hostState.flashDurationMillis.state.collectAsState()
    val flashMode by hostState.flashColor.state.collectAsState()
    val flashInterval by hostState.flashPageInterval.state.collectAsState()

    var currentColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(currentDisplayRefresh) {
        if (!currentDisplayRefresh) {
            currentColor = null
            return@LaunchedEffect
        }

        val refreshDurationHalf = refreshDuration.effectiveValue.milliseconds / 2
        currentColor = if (flashMode.effectiveValue == MangaReaderSettings.FlashColor.BLACK) {
            Color.Black
        } else {
            Color.White
        }
        delay(refreshDurationHalf)
        if (flashMode.effectiveValue == MangaReaderSettings.FlashColor.WHITE_BLACK) {
            currentColor = Color.Black
        }
        delay(refreshDurationHalf)
        hostState.currentDisplayRefresh = false
    }

    LaunchedEffect(flashInterval.effectiveValue) {
        hostState.setInterval(flashInterval.effectiveValue)
    }

    Canvas(
        modifier = modifier.fillMaxSize(),
    ) {
        currentColor?.let { drawRect(it) }
    }
}
