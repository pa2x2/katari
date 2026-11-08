package eu.kanade.presentation.more.settings.screen.textrecognition.language

import app.katari.language.api.tag.LanguageTag

/** The content language each library series' source declares, one per series, once sources are loaded. */
fun interface LibrarySeriesLanguages {
    suspend fun languages(): List<LanguageTag>
}
