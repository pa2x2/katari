package mihon.gradle.tasks

internal fun checkEntryMigrationBoundaries(
    sources: List<EntryMigrationBoundarySource>,
): List<EntryMigrationBoundaryFinding> {
    val findings = mutableListOf<EntryMigrationBoundaryFinding>()
    val hostApiNames = sources
        .asSequence()
        .filter { it.relativePath.startsWith(ENTRY_MIGRATION_HOST_API_ROOT) }
        .flatMap { it.declarations.asSequence() }
        .filter(EntryMigrationBoundaryDeclaration::isPublic)
        .map(EntryMigrationBoundaryDeclaration::name)
        .toSet()

    sources.forEach { source ->
        if (!source.isTestPath()) {
            checkMigrationHostReferences(source, hostApiNames, findings)
            checkRawMigrationAuthority(source, findings)
            checkOwnedMigrationImplementation(source, findings)
        }
    }

    return findings
}

private fun checkMigrationHostReferences(
    source: EntryMigrationBoundarySource,
    hostApiNames: Set<String>,
    findings: MutableList<EntryMigrationBoundaryFinding>,
) {
    hostApiNames.forEach { name ->
        if (source.isAllowedMigrationHostReference(name)) return@forEach
        source.references[name]?.let { lineNumber ->
            findings += EntryMigrationBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = "$name is an application host port reserved for the root Migration coordinator and " +
                    "the segregated app host adapter package",
            )
        }
    }
}

private fun checkRawMigrationAuthority(
    source: EntryMigrationBoundarySource,
    findings: MutableList<EntryMigrationBoundaryFinding>,
) {
    if (!source.isMigrationAuthorityGuardedPath()) return

    RAW_MIGRATION_AUTHORITY_NAMES.forEach { name ->
        source.references[name]?.let { lineNumber ->
            findings += EntryMigrationBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = "$name must be consumed through EntryMigrationFeature intent or preparation results",
            )
        }
    }
}

private fun checkOwnedMigrationImplementation(
    source: EntryMigrationBoundarySource,
    findings: MutableList<EntryMigrationBoundaryFinding>,
) {
    if (!source.isOwnedMigrationImplementationPath()) return

    AMBIENT_MIGRATION_AUTHORITY_NAMES.forEach { name ->
        source.references[name]?.let { lineNumber ->
            findings += EntryMigrationBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = lineNumber,
                reason = "Migration implementation must use captured intent, not ambient authority: $name",
            )
        }
    }

    source.content.lines().forEachIndexed { index, line ->
        val type = CONCRETE_ENTRY_TYPE_REFERENCE.find(line)?.groupValues?.get(1) ?: return@forEachIndexed
        findings += EntryMigrationBoundaryFinding(
            relativePath = source.relativePath,
            lineNumber = index + 1,
            reason = "Migration implementation cannot authorize behavior with a concrete EntryType: $type",
        )
    }
}

private fun EntryMigrationBoundarySource.isAllowedMigrationHostReference(name: String): Boolean {
    if (!name.startsWith("EntryMigration")) return false
    val fileName = relativePath.substringAfterLast("/")
    if (relativePath.startsWith(ENTRY_MIGRATION_HOST_API_ROOT)) return true
    if (relativePath.startsWith(ENTRY_MIGRATION_IMPLEMENTATION_ROOT)) return true
    if (relativePath == ENTRY_INTERACTION_RUNTIME_PATH) return true
    return relativePath.startsWith(APPLICATION_ENTRY_INTERACTION_HOST_ROOT) &&
        fileName.startsWith("AppEntryMigration") &&
        fileName.endsWith("Host.kt")
}

private fun EntryMigrationBoundarySource.isMigrationAuthorityGuardedPath(): Boolean {
    return relativePath.startsWith("app/src/main/") ||
        relativePath.startsWith("data/src/main/") ||
        relativePath.startsWith("domain/src/main/") ||
        relativePath.startsWith(ENTRY_MIGRATION_APPLICATION_API_ROOT)
}

private fun EntryMigrationBoundarySource.isOwnedMigrationImplementationPath(): Boolean {
    return relativePath.startsWith(ENTRY_MIGRATION_IMPLEMENTATION_ROOT) ||
        relativePath.startsWith(ENTRY_MIGRATION_APPLICATION_API_ROOT) ||
        relativePath == LEGACY_MIGRATION_USE_CASE_PATH ||
        (
            relativePath.startsWith(APPLICATION_ENTRY_INTERACTION_HOST_ROOT) &&
                relativePath.substringAfterLast("/").startsWith("AppEntryMigration")
            )
}

private fun EntryMigrationBoundarySource.isTestPath(): Boolean = relativePath.contains("/src/test/")

internal data class EntryMigrationBoundarySource(
    val relativePath: String,
    val content: String,
    val declarations: List<EntryMigrationBoundaryDeclaration>,
    val references: Map<String, Int>,
)

internal data class EntryMigrationBoundaryDeclaration(
    val name: String,
    val isPublic: Boolean,
    val lineNumber: Int,
)

internal data class EntryMigrationBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int,
    val reason: String,
)

private const val ENTRY_MIGRATION_APPLICATION_API_ROOT =
    "entry-interactions/api/src/main/java/mihon/entry/interactions/migration/"

private const val ENTRY_MIGRATION_HOST_API_ROOT =
    "entry-interactions/api/src/main/java/mihon/entry/interactions/migration/host/"

private const val ENTRY_MIGRATION_IMPLEMENTATION_ROOT =
    "entry-interactions/src/main/java/mihon/entry/interactions/migration/"

private const val ENTRY_INTERACTION_RUNTIME_PATH =
    "entry-interactions/src/main/java/mihon/entry/interactions/runtime/EntryInteractionRuntime.kt"

private const val APPLICATION_ENTRY_INTERACTION_HOST_ROOT =
    "app/src/main/java/mihon/entry/interactions/host/"

private const val LEGACY_MIGRATION_USE_CASE_PATH =
    "app/src/main/java/mihon/domain/migration/usecases/MigrateEntryUseCase.kt"

private val RAW_MIGRATION_AUTHORITY_NAMES = setOf(
    "EntryCapabilityInteraction",
    "MigrateEntryUseCase",
    "supportsMigration",
    "canMigrate",
    "migrationEntries",
)

private val AMBIENT_MIGRATION_AUTHORITY_NAMES = setOf(
    "ActiveProfileProvider",
    "ProfileAwareStore",
    "ProfileManager",
    "ProfileStore",
    "SourcePreferences",
    "activeProfileId",
    "currentProfileId",
    "migrationFlags",
)

private val CONCRETE_ENTRY_TYPE_REFERENCE = Regex("""\bEntryType\.([A-Z][A-Z0-9_]*)\b""")
