package mihon.desktop.textrecognition

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.more.settings.screen.textrecognition.language.LibrarySeriesLanguages
import mihon.language.api.tag.LanguageTag

/**
 * Desktop loads no sources, so no series' source declares its language. Stands in for the catalogue's descriptions of
 * sources, which reach sources through Android's extension loading, until desktop loads extensions.
 */
@Inject
class NoLibrarySeriesLanguages : LibrarySeriesLanguages {
    override suspend fun languages(): List<LanguageTag> = emptyList()
}
