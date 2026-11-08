package eu.kanade.tachiyomi.extension.kind

import dev.icerock.moko.resources.StringResource

/**
 * A family of extensions the platform runs, named [title], and the versions of the [library] they're built with that it
 * runs, `major.minor`, or `major` for libraries numbered that way.
 */
data class ExtensionFamilySupport(
    val title: StringResource,
    val library: String,
    val libVersions: List<String>,
)
