package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.DecoderInputBuffer
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.source.SampleStream
import androidx.media3.extractor.text.CueDecoder

/**
 * A subtitle stream that keeps every line it has read, so the text renderer can be given lines again. Media3's text
 * renderer reads its stream only forward and drops lines once they are over, so a timing moved later, or a seek back
 * over lines it has dropped, would leave lines due on screen unshown. This reads all [upstream] has loaded as soon as
 * it can, as subtitles are small, and hands the renderer the lines still showing at or after the position [rewind]
 * was last given, in time order, and then each line loaded later. Its positions are the stream's own times.
 *
 * Only for subtitles parsed to cues while loading (`MimeTypes.APPLICATION_MEDIA3_CUES`), whose samples say how long
 * they show; a sample that doesn't shows until the next one.
 */
@OptIn(markerClass = [UnstableApi::class])
internal class RecordedSubtitleStream(val upstream: SampleStream) : SampleStream {
    // Subtitle samples carry no flag but the key frame one, which every one of them has
    private class Line(val timeUs: Long, val data: ByteArray, val endUs: Long)

    private val cueDecoder = CueDecoder()
    private val scratch = DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL)
    private val scratchFormat = FormatHolder()

    // Every line read, by start time
    private val lines = ArrayList<Line>()

    // The lines not yet handed out since the last rewind, by start time
    private val pending = ArrayList<Line>()
    private var fromUs = Long.MIN_VALUE
    private var upstreamEnded = false

    /** Hands out again, from the next read, the lines still showing at [positionUs] and every one after them. */
    fun rewind(positionUs: Long) {
        fromUs = positionUs
        // After a seek the upstream stream is read again from its new position, and its end is to be read anew
        upstreamEnded = false
        pending.clear()
        lines.forEachIndexed { index, line -> if (showsUntil(index) > positionUs) pending.add(line) }
    }

    /** Whether a line not yet handed out starts by [positionUs]. */
    fun hasLineBy(positionUs: Long): Boolean {
        readUpstream()
        return pending.firstOrNull()?.let { it.timeUs <= positionUs } == true
    }

    override fun isReady(): Boolean = pending.isNotEmpty() || upstreamEnded || upstream.isReady

    override fun maybeThrowError() = upstream.maybeThrowError()

    override fun readData(formatHolder: FormatHolder, buffer: DecoderInputBuffer, readFlags: Int): Int {
        if (readFlags and SampleStream.FLAG_REQUIRE_FORMAT != 0) {
            return upstream.readData(formatHolder, buffer, readFlags)
        }
        readUpstream()
        val line = pending.firstOrNull()
        if (line == null) {
            if (!upstreamEnded) return C.RESULT_NOTHING_READ
            buffer.setFlags(C.BUFFER_FLAG_END_OF_STREAM)
            buffer.timeUs = C.TIME_END_OF_SOURCE
            return C.RESULT_BUFFER_READ
        }
        buffer.setFlags(C.BUFFER_FLAG_KEY_FRAME)
        buffer.timeUs = line.timeUs
        if (readFlags and SampleStream.FLAG_OMIT_SAMPLE_DATA == 0) {
            buffer.ensureSpaceForWrite(line.data.size)
            buffer.data!!.put(line.data)
        }
        if (readFlags and SampleStream.FLAG_PEEK == 0) pending.removeAt(0)
        return C.RESULT_BUFFER_READ
    }

    // The renderer is moved by rewind, which keeps lines that started before the position but still show there
    override fun skipData(positionUs: Long): Int = 0

    private fun readUpstream() {
        while (!upstreamEnded) {
            scratch.clear()
            when (upstream.readData(scratchFormat, scratch, 0)) {
                C.RESULT_BUFFER_READ -> if (scratch.isEndOfStream) upstreamEnded = true else record()
                C.RESULT_FORMAT_READ -> Unit
                else -> return
            }
        }
    }

    private fun record() {
        scratch.flip()
        val bytes = scratch.data!!.let { data -> ByteArray(data.remaining()).also(data::get) }
        val timeUs = scratch.timeUs
        var index = lines.binarySearchBy(timeUs) { it.timeUs }.let { if (it < 0) -it - 1 else it }
        while (index > 0 && lines[index - 1].timeUs == timeUs) index--
        // A seek back reads lines this has already
        while (index < lines.size && lines[index].timeUs == timeUs) {
            if (lines[index].data.contentEquals(bytes)) return
            index++
        }
        val endUs = cueDecoder.decode(timeUs, bytes, 0, bytes.size).endTimeUs
        val line = Line(timeUs, bytes, endUs)
        lines.add(index, line)
        if (showsUntil(index) > fromUs) {
            val at = pending.binarySearchBy(timeUs) { it.timeUs }.let { if (it < 0) -it - 1 else it + 1 }
            pending.add(at, line)
        }
    }

    // A line without an end shows until the next one starts
    private fun showsUntil(index: Int): Long {
        val line = lines[index]
        if (line.endUs != C.TIME_UNSET) return line.endUs
        return lines.getOrNull(index + 1)?.timeUs ?: Long.MAX_VALUE
    }
}
