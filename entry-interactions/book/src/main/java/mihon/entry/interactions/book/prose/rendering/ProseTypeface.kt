package mihon.entry.interactions.book.prose

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.createValidatedProseTypeface
import java.io.File
import java.security.MessageDigest

@Composable
internal fun rememberProseTypeface(
    loader: BookPublicationResourceLoader?,
    family: BookDocumentFontFamily.Resource?,
): androidx.compose.runtime.State<Typeface?> {
    val context = LocalContext.current
    return produceState<Typeface?>(
        initialValue = null,
        loader,
        family?.resourceId,
    ) {
        val resourceId = family?.resourceId ?: return@produceState
        value = loader?.loadProseTypeface(context, resourceId)?.getOrNull()
    }
}

@Composable
internal fun rememberInlineProseTypefaces(
    loader: BookPublicationResourceLoader?,
    styles: List<BookDocumentInlineStyleRange>,
): androidx.compose.runtime.State<Map<String, Typeface>> {
    val context = LocalContext.current
    val resourceIds = styles.mapNotNullTo(linkedSetOf()) { inline ->
        (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId
    }
    return produceState(
        initialValue = emptyMap(),
        loader,
        resourceIds,
    ) {
        if (loader == null) return@produceState
        val loaded = mutableMapOf<String, Typeface>()
        resourceIds.forEach { resourceId ->
            loader.loadProseTypeface(context, resourceId).getOrNull()?.let { typeface ->
                loaded[resourceId] = typeface
                value = loaded.toMap()
            }
        }
    }
}

internal fun Spanned.withInlineDocumentStyles(
    styles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
): Spanned {
    if (styles.isEmpty()) return this
    return SpannableString(this).apply {
        styles.forEach { range ->
            val start = range.start.coerceIn(0, length)
            val end = range.endExclusive.coerceIn(start, length)
            if (end <= start) return@forEach
            val spanFlags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            range.style.foregroundArgb?.let {
                setSpan(ForegroundColorSpan(it.toInt()), start, end, spanFlags)
            }
            range.style.backgroundArgb?.let {
                setSpan(BackgroundColorSpan(it.toInt()), start, end, spanFlags)
            }
            range.style.fontSizeScale?.let {
                setSpan(RelativeSizeSpan(it), start, end, spanFlags)
            }
            if (range.style.bold) {
                setSpan(StyleSpan(Typeface.BOLD), start, end, spanFlags)
            }
            if (range.style.italic) {
                setSpan(StyleSpan(Typeface.ITALIC), start, end, spanFlags)
            }
            if (range.style.underline) {
                setSpan(UnderlineSpan(), start, end, spanFlags)
            }
            if (range.style.strikethrough) {
                setSpan(StrikethroughSpan(), start, end, spanFlags)
            }
            if (range.style.subscript) {
                setSpan(SubscriptSpan(), start, end, spanFlags)
            }
            if (range.style.superscript) {
                setSpan(SuperscriptSpan(), start, end, spanFlags)
            }
            if (range.style.small && range.style.fontSizeScale == null) {
                setSpan(RelativeSizeSpan(SMALL_TEXT_SCALE), start, end, spanFlags)
            }
            if (range.style.code && range.style.fontFamily == null) {
                setSpan(TypefaceSpan("monospace"), start, end, spanFlags)
            }
            when (val family = range.style.fontFamily) {
                is BookDocumentFontFamily.Generic -> {
                    val name = when (family.family) {
                        BookDocumentFontFamily.GenericFamily.SERIF -> "serif"
                        BookDocumentFontFamily.GenericFamily.SANS_SERIF -> "sans-serif"
                        BookDocumentFontFamily.GenericFamily.MONOSPACE -> "monospace"
                    }
                    setSpan(TypefaceSpan(name), start, end, spanFlags)
                }
                is BookDocumentFontFamily.Resource -> inlineTypefaces[family.resourceId]?.let { typeface ->
                    setSpan(ProseTypefaceSpan(typeface), start, end, spanFlags)
                }
                null -> Unit
            }
        }
    }
}

internal fun Spanned.withInlineDocumentTypefaces(
    styles: List<BookDocumentInlineStyleRange>,
    inlineTypefaces: Map<String, Typeface>,
): Spanned {
    if (inlineTypefaces.isEmpty()) return this
    return SpannableString(this).apply {
        styles.forEach { range ->
            val family = range.style.fontFamily as? BookDocumentFontFamily.Resource
                ?: return@forEach
            val typeface = inlineTypefaces[family.resourceId] ?: return@forEach
            val start = range.start.coerceIn(0, length)
            val end = range.endExclusive.coerceIn(start, length)
            if (end > start) {
                setSpan(
                    ProseTypefaceSpan(typeface),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
    }
}

private class ProseTypefaceSpan(
    private val typeface: Typeface,
) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)
    override fun updateMeasureState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)

    private fun Paint.applyTypeface(newTypeface: Typeface) {
        val previousStyle = typeface?.style ?: Typeface.NORMAL
        val missingStyle = previousStyle and newTypeface.style.inv()
        if (missingStyle and Typeface.BOLD != 0) isFakeBoldText = true
        if (missingStyle and Typeface.ITALIC != 0) textSkewX = -0.25f
        typeface = newTypeface
    }
}

private const val SMALL_TEXT_SCALE = 0.8f

internal suspend fun BookPublicationResourceLoader.loadProseTypeface(
    context: android.content.Context,
    resourceId: String,
): Result<Typeface> {
    return try {
        val resource = load(
            resourceId,
            PROSE_FONT_RESOURCE_REQUIREMENT.acceptedMediaTypes,
            PROSE_FONT_RESOURCE_REQUIREMENT.maxBytes,
        ).getOrThrow()
        Result.success(
            withContext(Dispatchers.IO) {
                synchronized(proseFontCacheLock) {
                    val directory = File(context.cacheDir, "prose-fonts").apply { mkdirs() }
                    val digest = MessageDigest.getInstance("SHA-256")
                        .digest(resource.bytes)
                        .joinToString("") { byte -> "%02x".format(byte) }
                    val target = File(directory, digest)
                    pruneProseFontCache(directory, target)
                    if (!target.isFile || target.length() != resource.bytes.size.toLong()) {
                        target.outputStream().use { it.write(resource.bytes) }
                    }
                    target.setLastModified(System.currentTimeMillis())
                    createValidatedProseTypeface(target)
                }
            },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private fun pruneProseFontCache(directory: File, retained: File) {
    val files = directory.listFiles()
        .orEmpty()
        .filter(File::isFile)
        .sortedByDescending(File::lastModified)
    var retainedBytes = 0L
    var retainedFiles = 0
    files.forEach { file ->
        if (file == retained ||
            (retainedFiles < MAX_FONT_CACHE_FILES && retainedBytes + file.length() <= MAX_FONT_CACHE_BYTES)
        ) {
            retainedFiles++
            retainedBytes += file.length()
        } else {
            file.delete()
        }
    }
}

internal const val MAX_FONT_CACHE_FILES = 8
internal const val MAX_FONT_CACHE_BYTES = 16L * 1024L * 1024L
internal val proseFontCacheLock = Any()
