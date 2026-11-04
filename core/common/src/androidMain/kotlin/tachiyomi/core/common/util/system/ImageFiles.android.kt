package tachiyomi.core.common.util.system

import java.io.InputStream

actual fun isImageFile(name: String?, openStream: (() -> InputStream)?): Boolean = ImageUtil.isImage(name, openStream)

actual fun imageFileExtension(stream: InputStream): String? = ImageUtil.findImageType(stream)?.extension
