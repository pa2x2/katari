package tachiyomi.core.common.util.system

import java.io.InputStream

/**
 * Whether [name] is an image's, by its extension or else by the first bytes of what [openStream] opens. On Android
 * this is `ImageUtil.isImage`.
 */
expect fun isImageFile(name: String?, openStream: (() -> InputStream)? = null): Boolean
