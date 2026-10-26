package mihon.entry.interactions.book.document.reader.font

import android.app.Application
import android.graphics.fonts.Font
import android.net.Uri
import android.provider.OpenableColumns
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import java.io.File
import java.io.IOException

/**
 * The fonts each profile imported for the book reader: TTF and OTF files in the app's private storage, one folder per
 * profile. A file is kept only once Android loads it as a font, so a picked file that isn't one never reaches a page.
 */
internal class BookDocumentReaderFontStore(private val application: Application) : BookDocumentReaderFontLibrary {
    override fun importedFonts(profileId: Long): List<BookDocumentReaderFont.Imported> =
        directory(profileId).listFiles { file -> file.isFile && file.extension.lowercase() in EXTENSIONS }
            .orEmpty()
            .sortedBy { it.name.lowercase() }
            .map { BookDocumentReaderFont.Imported(it.name) }

    /** The font's file, or null once it is gone, such as after a restore, since imported files aren't backed up. */
    fun file(profileId: Long, font: BookDocumentReaderFont.Imported): File? =
        File(directory(profileId), font.fileName).takeIf { it.isFile }

    /** Copies the font at [uri] into the profile; null when the file isn't a font Android can load. */
    fun import(profileId: Long, uri: Uri): BookDocumentReaderFont.Imported? {
        val directory = directory(profileId).apply { mkdirs() }
        val pending = File.createTempFile("import", ".font", directory)
        try {
            application.contentResolver.openInputStream(uri)?.use { input ->
                pending.outputStream().use { input.copyTo(it) }
            } ?: return null
            try {
                Font.Builder(pending).build()
            } catch (_: IOException) {
                return null
            } catch (_: IllegalArgumentException) {
                return null
            }
            val target = uniqueFile(directory, displayName(uri))
            if (!pending.renameTo(target)) return null
            return BookDocumentReaderFont.Imported(target.name)
        } finally {
            pending.delete()
        }
    }

    fun delete(profileId: Long, font: BookDocumentReaderFont.Imported) {
        file(profileId, font)?.delete()
    }

    private fun directory(profileId: Long) = File(application.filesDir, "$DIRECTORY/$profileId")

    private fun displayName(uri: Uri): String {
        val name = application.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?: uri.lastPathSegment
        return name?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME
    }

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
