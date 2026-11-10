package app.katari.ui.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import eu.kanade.tachiyomi.source.entry.EntryType

/**
 * The type of the entry each composed screen is about, for [ShownScreenReport]. Kept here and not on the screens: a
 * screen is saved with the navigation by Java serialization, which fails on any Compose state it holds.
 */
@Stable
class ShownEntryTypes {
    private val types = mutableStateMapOf<ScreenKey, EntryType>()

    /** Null while [screen] is not composed or has not told its type yet. */
    operator fun get(screen: Screen): EntryType? = types[screen.key]

    internal fun set(screen: Screen, type: EntryType) {
        types[screen.key] = type
    }

    internal fun remove(screen: Screen) {
        types.remove(screen.key)
    }
}

val LocalShownEntryTypes = staticCompositionLocalOf<ShownEntryTypes?> { null }

/** Tells [ShownScreenReport] that [screen], while composed, is about an entry of [type]. */
@Composable
fun ReportEntryType(screen: Screen, type: EntryType) {
    val shownEntryTypes = LocalShownEntryTypes.current ?: return
    DisposableEffect(shownEntryTypes, screen, type) {
        shownEntryTypes.set(screen, type)
        onDispose { shownEntryTypes.remove(screen) }
    }
}
