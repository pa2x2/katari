package app.katari.entry.interactions.reader.window

import app.katari.language.api.tag.LanguageTag

/**
 * Where the desktop reader leaves for: back to the screen it was opened from, its settings, the settings its text
 * features need, the entry, the web view.
 */
interface MangaReaderNavigation {
    /** False where the app has no web view; the chapter's menu then leaves it out. */
    val webViewAvailable: Boolean

    fun close()

    fun openReaderSettings()

    /** Lets the user choose how [language] is read, and returns to the reader once the choice is stored. */
    fun openTextRecognitionPipelineChoice(language: LanguageTag)

    fun openTranslationSettings()

    fun openEntry(entryId: Long)

    /** Only called while [webViewAvailable]. */
    fun openWebView(url: String, title: String?, sourceId: Long?)
}
