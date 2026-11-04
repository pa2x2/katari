package mihon.video.engine.spi.player

/**
 * Why a player stopped. Each engine translates its own errors into a [category] once; what the viewer is told and
 * whether another engine or server is tried follow from the category alone. [codeName] is the engine's own name for
 * the error and, with the message and the causes, goes into a bug report.
 */
class VideoPlayerError(
    val category: Category,
    val codeName: String,
    message: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    enum class Category {
        /** The server couldn't be reached. */
        ConnectionFailed,
        ConnectionTimedOut,

        /** The server answered, but not with the stream: an HTTP error, wrong content, no such file or no access. */
        Rejected,

        /** Loading the stream failed some other way. */
        LoadFailed,

        /** The stream came, but the engine couldn't parse or decode it, which another engine may. */
        Unplayable,

        Other,
    }
}
