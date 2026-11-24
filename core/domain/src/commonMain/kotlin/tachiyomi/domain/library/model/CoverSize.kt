package tachiyomi.domain.library.model

/**
 * How large covers in an entry grid are, from the smallest to the largest. The grid fits as many columns of that size
 * as the width holds; covers grow in large and extra-large windows. A size stored by earlier versions as `Auto` reads as
 * [Medium], the default.
 */
enum class CoverSize {
    ExtraSmall,
    Small,
    Medium,
    Large,
    ExtraLarge,
    ;

    companion object {
        val default = Medium
    }
}
