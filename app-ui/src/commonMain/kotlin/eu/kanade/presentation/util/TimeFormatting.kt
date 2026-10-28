package eu.kanade.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLocale
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.domain.ui.UiPreferences
import mihon.app.ui.di.LocalAppUiGraph
import mihon.core.platform.currentPlatformContext
import tachiyomi.presentation.core.util.collectAsState
import java.time.format.DateTimeFormatter

/*
 * Times of day follow the Time format setting. Its "Follow system" choice reads the system's 12/24-hour setting, which
 * can change while the app is in the background, so formats built from it are rebuilt when the app resumes.
 */

@Composable
fun rememberIs24HourSystem(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = currentPlatformContext()
    var is24 by remember { mutableStateOf(context.is24HourSystem()) }
    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                is24 = context.is24HourSystem()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return is24
}

/** See [UiPreferences.hourSkeleton]. */
@Composable
fun rememberHourSkeleton(): String {
    val graph = LocalAppUiGraph.current
    val timeFormat by remember { graph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, is24HourSystem) { UiPreferences.hourSkeleton(timeFormat, is24HourSystem) }
}

/** See [UiPreferences.uses24Hours]. */
@Composable
fun rememberUses24Hours(): Boolean {
    val graph = LocalAppUiGraph.current
    val locale = LocalLocale.current.platformLocale
    val timeFormat by remember { graph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.uses24Hours(timeFormat, locale, is24HourSystem)
    }
}

@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val graph = LocalAppUiGraph.current
    val locale = LocalLocale.current.platformLocale
    val timeFormat by remember { graph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale, is24HourSystem)
    }
}

@Composable
fun rememberDateTimeFormatter(): DateTimeFormatter {
    val graph = LocalAppUiGraph.current
    val locale = LocalLocale.current.platformLocale
    val uiPreferences = remember { graph.uiPreferences }
    val dateFormat by uiPreferences.dateFormat.collectAsState()
    val timeFormat by uiPreferences.timeFormat.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(dateFormat, timeFormat, locale, is24HourSystem) {
        UiPreferences.dateTimeFormat(dateFormat, timeFormat, locale, is24HourSystem)
    }
}
