package eu.kanade.tachiyomi.source

import tachiyomi.domain.source.model.SourceDisplayInfo

fun SourceDisplayInfo.visualName(): String =
    if (lang.isNotBlank()) "$name (${lang.uppercase()})" else name

fun SourceDisplayInfo.sourceNotInstalledName(): String =
    if (lang.isNotBlank()) visualName() else name
