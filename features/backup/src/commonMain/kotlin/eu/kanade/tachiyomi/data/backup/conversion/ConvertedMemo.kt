package eu.kanade.tachiyomi.data.backup.conversion

import app.katari.core.common.extensions.JsonObjectEmptyBytes
import tachiyomi.data.MemoColumnAdapter

/** The memo as restoring can read it: restore decodes it as a JSON object, which an app's empty or malformed one isn't. */
internal fun ByteArray.validMemo(): ByteArray =
    takeIf { runCatching { MemoColumnAdapter.decode(it) }.isSuccess } ?: JsonObjectEmptyBytes
