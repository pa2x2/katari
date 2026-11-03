package eu.kanade.tachiyomi.source

import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.source.model.SourceDisplayInfo
import tachiyomi.domain.source.model.StubSource
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.source.local.LocalSource

fun Source.getDisplayNameForEntryInfo(preferences: SourcePreferences): String {
    return SourceDisplayInfo(
        id = id,
        name = name,
        lang = lang,
        isMissing = this is StubSource,
        hasKnownName = name.isNotBlank(),
    ).getDisplayNameForEntryInfo(preferences)
}

fun Source?.isLocalOrStub(): Boolean = this == null || id == LocalSource.ID || this is StubSource

fun UnifiedSource?.isLocalOrStub(): Boolean = this == null || id == LocalSource.ID || this is UnifiedStubSource
