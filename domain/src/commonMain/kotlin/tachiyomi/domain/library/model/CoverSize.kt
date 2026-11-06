package tachiyomi.domain.library.model

/**
 * How large covers in an entry grid are. The grid fits as many columns of that size as the width holds; covers grow
 * in large and extra-large windows, and [Auto] is the grid's own size.
 */
enum class CoverSize {
    Auto,
    Small,
    Medium,
    Large,
    ExtraLarge,
}
