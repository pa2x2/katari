package mihon.desktop.book

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary

/** Desktop has no book reader yet, so no profile has imported fonts. Stands in until desktop reads books. */
@Inject
class NoBookDocumentReaderFontLibrary : BookDocumentReaderFontLibrary {
    override fun importedFonts(profileId: Long): List<BookDocumentReaderFont.Imported> = emptyList()

    override fun deleteProfile(profileId: Long) = Unit
}
