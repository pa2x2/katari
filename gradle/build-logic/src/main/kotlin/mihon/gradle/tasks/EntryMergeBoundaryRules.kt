package mihon.gradle.tasks

internal fun checkEntryMergeBoundaries(
    sources: List<EntryMergeBoundarySource>,
): List<EntryMergeBoundaryFinding> {
    val findings = mutableListOf<EntryMergeBoundaryFinding>()
    val hostApiNames = sources
        .asSequence()
        .filter { it.relativePath.startsWith(ENTRY_INTERACTIONS_HOST_API_ROOT) }
        .flatMap { it.declarations.asSequence() }
        .filter(EntryMergeBoundaryDeclaration::isPublic)
        .map(EntryMergeBoundaryDeclaration::name)
        .toSet()

    sources.forEach { source ->
        if (!source.isTestPath()) {
            checkHostApiReferences(source, hostApiNames, findings)
            checkRawAuthorityReferences(source, findings)
            checkTransitionalSupportReferences(source, findings)
            checkApplicationApiSurface(source, findings)
            checkProfileMovePortReferences(source, findings)
            checkOwnedMergeImplementation(source, findings)
        }
    }

    return findings
}

private fun checkOwnedMergeImplementation(
    source: EntryMergeBoundarySource,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    if (!source.isMergeImplementationPath()) return

    AMBIENT_PROFILE_AUTHORITY_NAMES.forEach { name ->
        source.references[name]?.let { lineNumber ->
            findings += EntryMergeBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason =
                "Merge implementation must use captured profile identity, not ambient profile authority: $name",
            )
        }
    }

    source.content.lines().forEachIndexed { index, line ->
        val type = CONCRETE_ENTRY_TYPE_REFERENCE.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
        findings += EntryMergeBoundaryFinding(
            relativePath = source.relativePath,
            lineNumber = index + 1,
            reason = "Merge implementation cannot gate behavior on a concrete current EntryType: $type",
        )
    }
}

private fun checkProfileMovePortReferences(
    source: EntryMergeBoundarySource,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    if (!source.relativePath.startsWith("app/src/main/")) return
    source.references["EntryMergeProfileMoveFeature"]?.let { lineNumber ->
        findings += EntryMergeBoundaryFinding(
            relativePath = source.relativePath,
            lineNumber = lineNumber,
            reason = "EntryMergeProfileMoveFeature is reserved for the Profile Move coordinator",
        )
    }
}

private fun checkTransitionalSupportReferences(
    source: EntryMergeBoundarySource,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    if (!source.isRawAuthorityGuardedPath() && !source.relativePath.startsWith("entry-interactions/spi/src/main/")) {
        return
    }
    TRANSITIONAL_MERGE_SUPPORT_NAMES.forEach { name ->
        source.references[name]?.let { lineNumber ->
            findings += EntryMergeBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = "Merge is a shared feature workflow and cannot be gated by transitional type support: $name",
            )
        }
    }
}

private fun checkHostApiReferences(
    source: EntryMergeBoundarySource,
    hostApiNames: Set<String>,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    if (source.relativePath.startsWith("entry-interactions/api/src/main/")) return

    hostApiNames.forEach { name ->
        if (source.isAllowedHostReference(name)) return@forEach
        source.references[name]?.let { lineNumber ->
            findings += EntryMergeBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = "$name is an application host port reserved for the root Merge coordinator and " +
                    "the segregated app host adapter package",
            )
        }
    }
}

private fun checkRawAuthorityReferences(
    source: EntryMergeBoundarySource,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    val isEntryInteractionsApi = source.relativePath.startsWith("entry-interactions/api/src/main/")
    if (source.relativePath.startsWith(APPLICATION_ENTRY_INTERACTION_HOST_ROOT)) return
    if (!isEntryInteractionsApi && !source.isRawAuthorityGuardedPath()) return

    RAW_MERGE_AUTHORITY_NAMES.forEach { name ->
        source.references[name]?.let { lineNumber ->
            findings += EntryMergeBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = if (isEntryInteractionsApi) {
                    "raw Merge authority cannot cross the Merge application or host API boundary: $name"
                } else {
                    "raw Merge authority must be consumed through Merge intents or an owned projection: $name"
                },
            )
        }
    }
}

private fun checkApplicationApiSurface(
    source: EntryMergeBoundarySource,
    findings: MutableList<EntryMergeBoundaryFinding>,
) {
    if (!source.relativePath.startsWith("entry-interactions/api/src/main/")) return
    if (source.relativePath.startsWith(ENTRY_INTERACTIONS_HOST_API_ROOT)) return
    if (!source.content.contains("EntryMerge")) return

    source.declarations
        .filter { declaration -> declaration.isPublic && declaration.name in RAW_MERGE_APPLICATION_TYPES }
        .forEach { declaration ->
            findings += EntryMergeBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = declaration.lineNumber,
                reason = "application-facing Merge API cannot expose raw membership type: ${declaration.name}",
            )
        }

    source.content.lines().forEachIndexed { index, line ->
        val method = RAW_MERGE_APPLICATION_METHOD.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
        findings += EntryMergeBoundaryFinding(
            relativePath = source.relativePath,
            lineNumber = index + 1,
            reason = "application-facing Merge API cannot expose raw membership operation: $method",
        )
    }
}

private fun EntryMergeBoundarySource.isAllowedHostReference(name: String): Boolean {
    if (!name.startsWith("EntryMerge")) return false
    val fileName = relativePath.substringAfterLast("/")
    if (relativePath.startsWith(ENTRY_INTERACTIONS_MERGE_IMPLEMENTATION_ROOT)) {
        return true
    }
    if (relativePath == ENTRY_INTERACTION_RUNTIME_PATH && fileName == "EntryInteractionRuntime.kt") return true
    return relativePath.startsWith(APPLICATION_ENTRY_INTERACTION_HOST_ROOT) &&
        fileName.contains("EntryMerge") &&
        fileName.endsWith("Host.kt")
}

private fun EntryMergeBoundarySource.isMergeImplementationPath(): Boolean {
    return relativePath.startsWith(ENTRY_INTERACTIONS_MERGE_IMPLEMENTATION_ROOT) ||
        relativePath.startsWith(ENTRY_INTERACTIONS_MERGE_API_ROOT) ||
        (
            relativePath.startsWith(APPLICATION_ENTRY_INTERACTION_HOST_ROOT) &&
                relativePath.substringAfterLast("/").contains("EntryMerge")
            )
}

private fun EntryMergeBoundarySource.isRawAuthorityGuardedPath(): Boolean {
    return relativePath.startsWith("app/src/main/") ||
        relativePath.startsWith("data/src/main/") ||
        relativePath.startsWith("domain/src/main/") ||
        relativePath.startsWith("entry-interactions/src/main/") ||
        ENTRY_TYPE_MODULE_ROOT.matches(relativePath)
}

private fun EntryMergeBoundarySource.isTestPath(): Boolean = relativePath.contains("/src/test/")

internal data class EntryMergeBoundarySource(
    val relativePath: String,
    val content: String,
    val declarations: List<EntryMergeBoundaryDeclaration>,
    val references: Map<String, Int>,
)

internal data class EntryMergeBoundaryDeclaration(
    val name: String,
    val isPublic: Boolean,
    val lineNumber: Int,
)

internal data class EntryMergeBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int,
    val reason: String,
)

private const val ENTRY_INTERACTIONS_HOST_API_ROOT =
    "entry-interactions/api/src/main/java/mihon/entry/interactions/merge/host/"

private const val ENTRY_INTERACTIONS_MERGE_API_ROOT =
    "entry-interactions/api/src/main/java/mihon/entry/interactions/merge/"

private const val ENTRY_INTERACTIONS_MERGE_IMPLEMENTATION_ROOT =
    "entry-interactions/src/main/java/mihon/entry/interactions/merge/"

private const val ENTRY_INTERACTION_RUNTIME_PATH =
    "entry-interactions/src/main/java/mihon/entry/interactions/runtime/EntryInteractionRuntime.kt"

private const val APPLICATION_ENTRY_INTERACTION_HOST_ROOT =
    "app/src/main/java/mihon/entry/interactions/host/"

private val ENTRY_TYPE_MODULE_ROOT = Regex(
    """entry-interactions/(?!api/|spi/|download-notification/|src/)[^/]+/src/main/.*""",
)

private val RAW_MERGE_AUTHORITY_NAMES = setOf(
    "GetMergedEntry",
    "UpdateMergedEntry",
    "MergedEntryRepository",
    "EntryMerge",
    "merged_entriesQueries",
)

private val TRANSITIONAL_MERGE_SUPPORT_NAMES = setOf(
    "EntryMergeProvider",
    "EntryMergeCapability",
    "EntryMergeCapabilityItem",
    "supportsMerge",
    "canMergeSelection",
)

private val AMBIENT_PROFILE_AUTHORITY_NAMES = setOf(
    "ActiveProfileProvider",
    "ProfileAwareStore",
    "ProfileManager",
    "ProfileStore",
    "activeProfileId",
    "currentProfileId",
)

private val CONCRETE_ENTRY_TYPE_REFERENCE = Regex("""\bEntryType\.([A-Z][A-Z0-9_]*)\b""")

private val RAW_MERGE_APPLICATION_TYPES = setOf(
    "EntryMergeGroup",
    "EntryMergeMember",
    "EntryMergeGroupChange",
    "EntryMergeRepository",
    "EntryMergeStore",
    "EntryMergeDataPort",
    "EntryMergeHost",
)

private val RAW_MERGE_APPLICATION_METHOD = Regex(
    """\bfun\s+(group|groups|membership|members|targetId|save|upsertGroup|deleteGroup|removeMembers|replaceMember)\s*\(""",
)
