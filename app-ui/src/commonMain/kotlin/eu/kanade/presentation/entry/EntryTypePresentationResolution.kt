package eu.kanade.presentation.entry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import app.katari.entry.interactions.presentation.EntryTypePresentationFeature
import app.katari.ui.di.LocalAppUiGraph
import eu.kanade.tachiyomi.source.entry.EntryType

typealias EntryTypePresentation = app.katari.entry.interactions.presentation.EntryTypePresentation

/**
 * Resolves contributed type vocabulary. Generic resolution retains the requested type so missing projection evidence
 * remains available to validation while every presentation surface receives renderable neutral vocabulary.
 */
fun EntryType?.entryTypePresentation(feature: EntryTypePresentationFeature): EntryTypePresentation {
    return feature.presentation(this).presentation
}

@Composable
@ReadOnlyComposable
fun EntryType?.entryTypePresentation(): EntryTypePresentation {
    return entryTypePresentation(LocalAppUiGraph.current.entryTypePresentationFeature)
}

fun Iterable<EntryType>.selectionEntryTypePresentation(feature: EntryTypePresentationFeature): EntryTypePresentation {
    return toSet().singleOrNull().entryTypePresentation(feature)
}

@Composable
@ReadOnlyComposable
fun Iterable<EntryType>.selectionEntryTypePresentation(): EntryTypePresentation {
    return selectionEntryTypePresentation(LocalAppUiGraph.current.entryTypePresentationFeature)
}
