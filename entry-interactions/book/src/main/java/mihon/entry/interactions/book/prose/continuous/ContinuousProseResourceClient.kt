package mihon.entry.interactions.book.prose.continuous

import android.graphics.Bitmap
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.createValidatedProseTypeface
import mihon.entry.interactions.book.document.resource.decodeValidatedProseImage
import tachiyomi.domain.entry.model.EntryChapter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

internal class ContinuousProseResourceClient {
    var projection: ContinuousProseProjection? = null
    var sections: Map<String, BookDocumentSection<EntryChapter>> = emptyMap()

    fun intercept(url: String): WebResourceResponse? {
        val prefix = "$CONTINUOUS_PROSE_ORIGIN/resource/"
        if (!url.startsWith(prefix)) return null
        val token = url.removePrefix(prefix).substringBefore('?')
        if (!RESOURCE_TOKEN.matches(token)) return forbidden()
        val spec = projection?.resources?.get(token) ?: return notFound()
        val section = sections[spec.sectionKey] ?: return notFound()
        val loader = section.resourceLoader ?: return notFound()
        return runCatching {
            when (spec.kind) {
                ContinuousProseResourceKind.IMAGE -> {
                    val loaded = runBlocking(Dispatchers.IO) {
                        loader.load(
                            spec.resourceId,
                            PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                            PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
                        )
                    }.getOrThrow()
                    val bitmap = decodeValidatedProseImage(
                        bytes = loaded.bytes,
                        targetWidthPx = MAX_WEB_IMAGE_DIMENSION,
                        targetHeightPx = MAX_WEB_IMAGE_DIMENSION,
                    )
                    val encoded = ByteArrayOutputStream().use { output ->
                        check(bitmap.compress(Bitmap.CompressFormat.PNG, WEB_IMAGE_QUALITY, output))
                        output.toByteArray()
                    }
                    bitmap.recycle()
                    require(encoded.size <= MAX_WEB_IMAGE_BYTES)
                    WebResourceResponse("image/png", null, ByteArrayInputStream(encoded))
                }
                ContinuousProseResourceKind.FONT -> {
                    val loaded = runBlocking(Dispatchers.IO) {
                        loader.load(
                            spec.resourceId,
                            PROSE_FONT_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                            PROSE_FONT_RESOURCE_REQUIREMENT.maxBytes,
                        )
                    }.getOrThrow()
                    validateFontBytes(loaded.bytes)
                    val mediaType = loaded.mediaType
                        ?.substringBefore(';')
                        ?.lowercase()
                        ?.takeIf { it in PROSE_FONT_RESOURCE_REQUIREMENT.acceptedMediaTypes }
                        ?: "font/ttf"
                    WebResourceResponse(mediaType, null, ByteArrayInputStream(loaded.bytes))
                }
            }
        }.getOrElse { notFound() }
    }

    private fun validateFontBytes(bytes: ByteArray) {
        require(bytes.size in 1..PROSE_FONT_RESOURCE_REQUIREMENT.maxBytes)
        val file = File.createTempFile("continuous-prose-font-", ".font")
        try {
            file.writeBytes(bytes)
            createValidatedProseTypeface(file)
        } finally {
            file.delete()
        }
    }

    private fun forbidden() = WebResourceResponse(
        "text/plain",
        "utf-8",
        403,
        "Forbidden",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0)),
    )

    private fun notFound() = WebResourceResponse(
        "text/plain",
        "utf-8",
        404,
        "Not found",
        emptyMap(),
        ByteArrayInputStream(ByteArray(0)),
    )

    private companion object {
        val RESOURCE_TOKEN = Regex("[0-9a-f]{32}")
        const val MAX_WEB_IMAGE_DIMENSION = 2_048
        const val MAX_WEB_IMAGE_BYTES = 16 * 1024 * 1024
        const val WEB_IMAGE_QUALITY = 100
    }
}
