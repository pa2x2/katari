package app.katari.sync.host

import app.katari.sync.client.file.local.FileOwner
import app.katari.sync.client.file.local.LocalFileStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.data.cache.CoverCache
import mihon.entry.interactions.reader.settings.BookDocumentReaderFont
import mihon.entry.interactions.reader.settings.BookDocumentReaderFontLibrary
import java.io.File

/** The files sync carries where the app keeps them: custom covers in the cover cache, fonts in the font library. */
@Inject
@ContributesBinding(AppScope::class)
class AppLocalFileStore(
    private val coverCache: CoverCache,
    private val fonts: BookDocumentReaderFontLibrary,
) : LocalFileStore {
    override fun file(owner: FileOwner): File? = when (owner) {
        is FileOwner.CustomCover -> coverCache.getCustomCoverFile(owner.entryId).takeIf { it.isFile }
        is FileOwner.Font -> fonts.file(owner.profileId, owner.font)
    }

    override fun put(owner: FileOwner, source: File) {
        when (owner) {
            is FileOwner.CustomCover -> source.inputStream().use { input ->
                coverCache.replaceCustomCover(owner.entryId, input, temporarySuffix = "sync")
            }
            is FileOwner.Font -> fonts.place(owner.profileId, owner.font, source)
        }
    }

    override fun remove(owner: FileOwner) {
        when (owner) {
            is FileOwner.CustomCover -> coverCache.deleteCustomCover(owner.entryId)
            is FileOwner.Font -> fonts.delete(owner.profileId, owner.font)
        }
    }

    override fun fonts(profileId: Long): List<String> = fonts.importedFonts(profileId).map { it.fileName }

    override fun watch(onChange: (FileOwner) -> Unit): AutoCloseable {
        val covers = coverCache.watchCustomCovers { onChange(FileOwner.CustomCover(it)) }
        val imported = fonts.watch { profileId, font -> onChange(FileOwner.Font(profileId, font.fileName)) }
        return AutoCloseable {
            covers.close()
            imported.close()
        }
    }

    private val FileOwner.Font.font: BookDocumentReaderFont.Imported
        get() = BookDocumentReaderFont.Imported(fileName)
}
