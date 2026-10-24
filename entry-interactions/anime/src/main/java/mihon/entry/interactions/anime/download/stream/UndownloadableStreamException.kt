package mihon.entry.interactions.anime.download.stream

/** A stream that plays but can't be kept as files. */
internal class UndownloadableStreamException(val reason: Reason) : Exception("Undownloadable stream: $reason") {
    enum class Reason {
        /** Its segments keep appearing, so there is no whole to keep. */
        LIVE,

        /** Only a DRM licence, which a download can't keep, decrypts it. */
        COPY_PROTECTED,
    }
}
