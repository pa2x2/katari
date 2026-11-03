package tachiyomi.domain.storage

/**
 * A document or folder the user picked, as a URI string: on Android the `content://` (or `file://`) URI the system
 * picker returned, on desktop a `file://` URI.
 */
@JvmInline
value class DocumentUri(val value: String)
