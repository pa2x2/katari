package eu.kanade.tachiyomi.ui.reader.viewer

import app.katari.entry.viewer.settings.ViewerSettingBinding

/** The value the opened entry uses now, its per-series override included, for code that runs on an event. */
internal val <T> ViewerSettingBinding<T>.current: T
    get() = state.value.effectiveValue
