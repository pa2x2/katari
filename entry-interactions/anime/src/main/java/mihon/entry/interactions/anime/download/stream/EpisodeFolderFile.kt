package mihon.entry.interactions.anime.download.stream

import com.hippo.unifile.UniFile
import java.io.IOException

/**
 * A new file in the episode's folder named exactly [name]. A playlist or manifest written beside it names its files,
 * so one the storage provider renamed (to avoid a clash, or to suit a MIME type) fails the download here instead of
 * leaving an episode that can't play.
 */
internal fun UniFile.createNamedFile(name: String): UniFile {
    val file = createFile(name) ?: throw IOException("Couldn't create '$name'")
    if (file.name != name) {
        file.delete()
        throw IOException("The storage named '$name' '${file.name}'")
    }
    return file
}
