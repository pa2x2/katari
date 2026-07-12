package tachiyomi.domain.entry.model

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType

@Immutable
data class EntryIdentity(
    val profileId: Long,
    val source: Long,
    val url: String,
    val type: EntryType,
)

fun Entry.identity(): EntryIdentity = EntryIdentity(
    profileId = profileId,
    source = source,
    url = url,
    type = type,
)
