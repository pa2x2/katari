package mihon.entry.viewer.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import mihon.entry.viewer.settings.shared.ReaderSharedSettingAvailability
import mihon.entry.viewer.settings.shared.ResolvedReaderSharedToggleSetting

@Composable
internal fun rememberReaderSharedSettingAvailability(
    setting: ResolvedReaderSharedToggleSetting,
): ReaderSharedSettingAvailability? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeGeneration by remember(setting) { mutableIntStateOf(0) }
    var availability by remember(setting) { mutableStateOf<ReaderSharedSettingAvailability?>(null) }

    DisposableEffect(lifecycleOwner, setting) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeGeneration++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(setting, resumeGeneration) {
        availability = setting.resolveAvailability()
    }
    LaunchedEffect(setting) {
        setting.availabilityChanges.collect {
            availability = setting.resolveAvailability()
        }
    }
    return availability
}
