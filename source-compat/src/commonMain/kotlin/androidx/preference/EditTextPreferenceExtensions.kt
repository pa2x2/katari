package androidx.preference

/**
 * Returns package-private [EditTextPreference.getOnBindEditTextListener], which only androidx's own dialog reads; the
 * source settings host runs the listener in its place.
 */
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
internal fun EditTextPreference.getOnBindEditTextListener(): EditTextPreference.OnBindEditTextListener? {
    return onBindEditTextListener
}
