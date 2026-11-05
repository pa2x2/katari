package mihon.entry.interactions.manga.reader.image

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext
import java.io.InputStream

/**
 * Hands a reader page's image out of the app, from the page actions: saved into the platform's pictures folder,
 * shared to an app the user picks, or copied to the clipboard. [image] opens the page's image each time it is read.
 * Each call throws when the image can't be read or written, for the caller to report.
 */
@PlatformFacility
internal interface MangaPageImageExport {
    /** False where the platform can't save pages; Save is then not offered. */
    val offersSaving: Boolean

    /** False where the platform has no share sheet; Share is then not offered. */
    val offersSharing: Boolean

    /** False where the platform can't put an image on the clipboard; Copy is then not offered. */
    val offersCopying: Boolean

    /** Saves the image as [name] into the pictures folder, inside [folder] there when given. */
    suspend fun save(context: PlatformContext, image: () -> InputStream, name: String, folder: String?)

    /** Hands the image, named [name], to an app the user picks, with [message] saying which page it is. */
    suspend fun share(context: PlatformContext, image: () -> InputStream, name: String, message: String)

    /** Puts the image, named [name], on the clipboard. */
    suspend fun copy(context: PlatformContext, image: () -> InputStream, name: String)
}
