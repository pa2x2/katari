package app.katari.sync.client.merge

import app.katari.sync.client.payload.Stamped

/**
 * The later of two edits of one field. Two devices comparing the same pair pick the same one, so devices that merged
 * the same edits in any order hold the same value.
 *
 * Timestamps differ for every real edit, since each carries its device. They tie for values neither device saw edited
 * while syncing ([Stamped.UNKNOWN_EDIT]), such as two devices' libraries merged at their first sync; the larger value
 * wins then, so a chapter read on either device stays read and the later of two dates stays.
 */
internal fun <T> latest(a: Stamped<T>, b: Stamped<T>): Stamped<T> = when {
    a.at > b.at -> a
    b.at > a.at -> b
    compareValues(a.value, b.value) >= 0 -> a
    else -> b
}

/** Orders values of one field: null first, comparable values naturally, anything else by its text. */
internal fun compareValues(a: Any?, b: Any?): Int = when {
    a == b -> 0
    a == null -> -1
    b == null -> 1
    a is Comparable<*> && a::class == b::class -> {
        @Suppress("UNCHECKED_CAST")
        (a as Comparable<Any>).compareTo(b)
    }
    else -> a.toString().compareTo(b.toString())
}

/** The larger of two values by [compareValues], for fields that carry no timestamp of their own. */
internal fun <T> larger(a: T, b: T): T = if (compareValues(a, b) >= 0) a else b
