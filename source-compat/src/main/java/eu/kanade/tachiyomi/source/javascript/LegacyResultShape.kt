package eu.kanade.tachiyomi.source.javascript

import app.cash.quickjs.QuickJsException

/**
 * Converts a quickjs-kt result to what the old binding returned, which extensions check with `is Array<*>`, `as Int`
 * and the like. QuickJS integers are 32-bit, so a `Long` always fits an `Int`. Arrays become `Object[]`, and an
 * element the old binding couldn't marshal (an object, a function) fails the call. Any other top-level value
 * (objects, functions) was `null`. A Promise was `null` too, but quickjs-kt describes it as a String, which can't be
 * told apart from a script's own string.
 */
internal fun Any?.toLegacyResult(): Any? = when (this) {
    is List<*> -> toLegacyArray()
    else -> toLegacyPrimitive { null }
}

private fun List<*>.toLegacyArray(): Array<Any?> = Array(size) { index ->
    when (val element = get(index)) {
        is List<*> -> element.toLegacyArray()
        else -> element.toLegacyPrimitive { throw QuickJsException("Cannot marshal value $element to Java") }
    }
}

private inline fun Any?.toLegacyPrimitive(otherwise: () -> Any?): Any? = when (this) {
    null, is Boolean, is Double, is String -> this
    is Long -> toInt()
    else -> otherwise()
}
