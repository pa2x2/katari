package eu.kanade.presentation.util

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import eu.kanade.domain.ui.UiPreferences
import mihon.app.di.appGraph
import tachiyomi.presentation.core.util.collectAsState
import java.time.format.DateTimeFormatter

/*
 * Times of day follow the Time format setting. Its "Follow system" choice reads the phone's 12/24-hour switch, which
 * can change while the app is in the background, so formats built from it are rebuilt when the app resumes.
 */

@Composable
fun rememberIs24HourSystem(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var is24 by remember { mutableStateOf(DateFormat.is24HourFormat(context)) }
    DisposableEffect(lifecycleOwner.lifecycle) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                is24 = DateFormat.is24HourFormat(context)
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
    val context = LocalContext.current
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, is24HourSystem) { UiPreferences.hourSkeleton(timeFormat, is24HourSystem) }
}

/** See [UiPreferences.uses24Hours]. */
@Composable
fun rememberUses24Hours(): Boolean {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.uses24Hours(timeFormat, locale, is24HourSystem)
    }
}

@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val timeFormat by remember { context.appGraph.uiPreferences.timeFormat }.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(timeFormat, locale, is24HourSystem) {
        UiPreferences.timeFormat(timeFormat, locale, is24HourSystem)
    }
}

@Composable
fun rememberDateTimeFormatter(): DateTimeFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val uiPreferences = remember { context.appGraph.uiPreferences }
    val dateFormat by uiPreferences.dateFormat.collectAsState()
    val timeFormat by uiPreferences.timeFormat.collectAsState()
    val is24HourSystem = rememberIs24HourSystem()
    return remember(dateFormat, timeFormat, locale, is24HourSystem) {
        UiPreferences.dateTimeFormat(dateFormat, timeFormat, locale, is24HourSystem)
    }
}
