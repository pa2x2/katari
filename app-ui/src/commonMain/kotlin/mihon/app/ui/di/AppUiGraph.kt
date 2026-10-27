package mihon.app.ui.di

import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import mihon.core.platform.clipboard.TextClipboard
import mihon.core.platform.link.LinkOpener
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import mihon.feature.profiles.ui.ProfilesGraph
import mihon.presentation.kit.theme.KatariThemeGraph

/**
 * What the app's shared screens reach from the graph of the platform they run on. Each platform's application graph
 * extends it and provides it to composition through [LocalAppUiGraph].
 */
interface AppUiGraph :
    ViewModelGraph,
    KatariThemeGraph,
    ProfilesGraph {
    val viewModelFactory: MetroViewModelFactory
    val entryTypePresentationFeature: EntryTypePresentationFeature
    val linkOpener: LinkOpener
    val textClipboard: TextClipboard
}
