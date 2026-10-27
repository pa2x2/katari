package mihon.video.engine.mpv.quality

import kotlin.math.sqrt

/**
 * The network throughput mpv's streams arrive at, measured by the stream proxy, as ExoPlayer's bandwidth meter
 * measures its own: the median of recent samples, each weighted by the square root of its size.
 *
 * Only time spent waiting on upstream reads counts. mpv stops reading once its cache is full, and the time the proxy
 * then waits to hand over data says nothing about the network. Overlapping reads count their shared time once.
 *
 * One meter serves every playback, so a stream starts from what the streams before it measured.
 */
internal class MpvBandwidthMeter {

    private val samples = WeightedMedian(MAX_SAMPLE_WEIGHT)
    private var activeReads = 0
    private var markNanos = 0L
    private var pendingBytes = 0L
    private var pendingNanos = 0L

    /** Bits per second, or null before anything was measured. */
    val bitrateEstimate: Long?
        @Synchronized get() = samples.median()?.toLong()

    /** Runs [read], an upstream read that returns the bytes it read or -1 at the end, and measures it. */
    fun measure(read: () -> Int): Int {
        started()
        var bytes = 0
        try {
            bytes = read()
        } finally {
            finished(bytes.coerceAtLeast(0))
        }
        return bytes
    }

    @Synchronized
    private fun started() {
        advance()
        activeReads++
    }

    @Synchronized
    private fun finished(bytes: Int) {
        advance()
        activeReads--
        pendingBytes += bytes
        if (pendingNanos > 0 && (pendingBytes >= SAMPLE_BYTES || pendingNanos >= SAMPLE_NANOS)) {
            val bitsPerSecond = pendingBytes * BITS_PER_BYTE * NANOS_PER_SECOND / pendingNanos
            samples.add(weight = sqrt(pendingBytes.toDouble()), value = bitsPerSecond)
            pendingBytes = 0
            pendingNanos = 0
        }
    }

    private fun advance() {
        val now = System.nanoTime()
        if (activeReads > 0) pendingNanos += now - markNanos
        markNanos = now
    }

    private companion object {
        // ExoPlayer's DefaultBandwidthMeter values.
        const val MAX_SAMPLE_WEIGHT = 2000.0
        const val SAMPLE_BYTES = 512L * 1024
        const val SAMPLE_NANOS = 2_000_000_000L

        const val BITS_PER_BYTE = 8.0
        const val NANOS_PER_SECOND = 1e9
    }
}

/** The weighted median of the latest samples whose weights add up to at most [maxWeight]. */
private class WeightedMedian(private val maxWeight: Double) {

    private class Sample(var weight: Double, val value: Double)

    private val samples = ArrayDeque<Sample>()
    private var totalWeight = 0.0

    fun add(weight: Double, value: Double) {
        samples.addLast(Sample(weight, value))
        totalWeight += weight
        while (totalWeight > maxWeight) {
            val oldest = samples.first()
            val excess = totalWeight - maxWeight
            if (oldest.weight <= excess) {
                samples.removeFirst()
                totalWeight -= oldest.weight
            } else {
                oldest.weight -= excess
                totalWeight -= excess
            }
        }
    }

    fun median(): Double? {
        if (samples.isEmpty()) return null
        val half = totalWeight / 2
        var cumulative = 0.0
        val sorted = samples.sortedBy { it.value }
        for (sample in sorted) {
            cumulative += sample.weight
            if (cumulative >= half) return sample.value
        }
        return sorted.last().value
    }
}
