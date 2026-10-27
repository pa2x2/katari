package mihon.domain.extension.model

enum class ContentWarning {
    SAFE,
    MIXED,
    NSFW,

    /** Its store doesn't rate its extensions, so whether its sources have 18+ content isn't known. */
    NOT_RATED,
}
