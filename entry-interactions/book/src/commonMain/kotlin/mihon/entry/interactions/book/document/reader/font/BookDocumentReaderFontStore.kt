package mihon.entry.interactions.book.document.reader.font

import mihon.core.platform.storage.AppPrivateDirectories
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import java.io.File
import java.io.InputStream

/**
 * The fonts each profile imported for the book reader: TTF and OTF files in the app's private files, one folder per
 * profile. A file is kept only once the platform loads it as a font, so a picked file that isn't one never reaches a
 * page.
 */
internal class BookDocumentReaderFontStore(private val directories: AppPrivateDirectories) :
    BookDocumentReaderFontLibrary {
    override fun importedFonts(profileId: Long): List<BookDocumentReaderFont.Imported> =
        directory(profileId).listFiles { file -> file.isFile && file.extension.lowercase() in EXTENSIONS }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map { BookDocumentReaderFont.Imported(it.name) }

    /** The font's file, or null once it is gone, such as after a restore, since imported files aren't backed up. */
    fun file(profileId: Long, font: BookDocumentReaderFont.Imported): File? =
        File(directory(profileId), font.fileName).takeIf { it.isFile }

    /**
     * Copies the picked file into the profile under the name [displayName] gives it; null when [open] has nothing to
     * read or [isFont] rejects the copy.
     */
    fun import(
        profileId: Long,
        open: () -> InputStream?,
        isFont: (File) -> Boolean,
        displayName: () -> String?,
    ): BookDocumentReaderFont.Imported? {
        val directory = directory(profileId).apply { mkdirs() }
        val pending = File.createTempFile("import", ".font", directory)
        try {
            open()?.use { input ->
                pending.outputStream().use { input.copyTo(it) }
            } ?: return null
            if (!isFont(pending)) return null
            val name = displayName()?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
            val target = uniqueFile(directory, name)
            if (!pending.renameTo(target)) return null
            return BookDocumentReaderFont.Imported(target.name)
        } finally {
            pending.delete()
        }
    }

    fun delete(profileId: Long, font: BookDocumentReaderFont.Imported) {
        file(profileId, font)?.delete()
    }

    override fun deleteProfile(profileId: Long) {
        directory(profileId).deleteRecursively()
    }

    private fun directory(profileId: Long) = File(directories.files, "$DIRECTORY/$profileId")

    private fun uniqueFile(directory: File, displayName: String): File {
        val extension = displayName.substringAfterLast('.', "").lowercase().takeIf { it in EXTENSIONS } ?: "ttf"
        val base = displayName.substringBeforeLast('.').replace(UNSAFE_CHARACTERS, "_").ifBlank { DEFAULT_NAME }
        return generateSequence(1) { it + 1 }
            .map { copy -> File(directory, if (copy == 1) "$base.$extension" else "$base ($copy).$extension") }
            .first { !it.exists() }
    }

    private companion object {
        const val DIRECTORY = "book-reader-fonts"
        const val DEFAULT_NAME = "Font"
        val EXTENSIONS = setOf("ttf", "otf")
        val UNSAFE_CHARACTERS = Regex("""[/\\:*?"<>|\u0000-\u001f]""")
    }
}
