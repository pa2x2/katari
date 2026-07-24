package mihon.gradle.tasks

internal fun checkEntryViewerSettingsProjectionBoundaries(
    sources: List<EntryViewerSettingsProjectionBoundarySource>,
): List<EntryViewerSettingsProjectionBoundaryFinding> {
    val productionSources = sources.filter { source -> "/src/main/" in source.relativePath }
    val declarations = productionSources.flatMap { source ->
        APP_VIEWER_SETTINGS_SCREEN_DECLARATION.findAll(source.content).map { match ->
            ViewerSettingsScreenDeclaration(
                name = match.groupValues[1],
                source = source,
                offset = match.range.first,
            )
        }
    }
    if (declarations.isEmpty()) return emptyList()

    val findings = mutableListOf<EntryViewerSettingsProjectionBoundaryFinding>()
    val registry = productionSources.firstOrNull { source ->
        source.relativePath == VIEWER_SETTINGS_SCREEN_REGISTRY
    }
    if (registry == null) {
        return listOf(
            EntryViewerSettingsProjectionBoundaryFinding(
                relativePath = VIEWER_SETTINGS_SCREEN_REGISTRY,
                lineNumber = null,
                reason = "Viewer Settings screen projections require one build-enforced production registry",
            ),
        )
    }

    declarations.forEach { declaration ->
        val count = Regex("""\b${Regex.escape(declaration.name)}\b""")
            .findAll(registry.content)
            .count()
        when {
            count == 0 -> findings += declaration.finding(
                "Viewer Settings screen projection is missing from the production resolver: ${declaration.name}",
            )
            count > 1 -> findings += registry.finding(
                registry.content.indexOf(declaration.name),
                "Viewer Settings screen projection is registered more than once: ${declaration.name}",
            )
        }
    }

    val declarationNames = declarations.mapTo(mutableSetOf(), ViewerSettingsScreenDeclaration::name)
    VIEWER_SETTINGS_SCREEN_REFERENCE.findAll(registry.content)
        .map { match -> match.groupValues[1] to match.range.first }
        .filter { (name, _) -> name !in declarationNames }
        .forEach { (name, offset) ->
            findings += registry.finding(
                offset,
                "Viewer Settings production resolver names no screen projection declaration: $name",
            )
        }

    val appModule = productionSources.firstOrNull { source -> source.relativePath == APP_MODULE }
    if (appModule == null || !appModule.content.contains("productionEntryViewerSettingsScreenProjectionResolver()")) {
        findings += EntryViewerSettingsProjectionBoundaryFinding(
            relativePath = APP_MODULE,
            lineNumber = null,
            reason = "AppModule must install the build-enforced Viewer Settings screen projection resolver",
        )
    }

    return findings.distinct()
}

private fun ViewerSettingsScreenDeclaration.finding(
    reason: String,
): EntryViewerSettingsProjectionBoundaryFinding = source.finding(offset, reason)

private fun EntryViewerSettingsProjectionBoundarySource.finding(
    offset: Int,
    reason: String,
): EntryViewerSettingsProjectionBoundaryFinding {
    return EntryViewerSettingsProjectionBoundaryFinding(
        relativePath = relativePath,
        lineNumber = content.take(offset).count { character -> character == '\n' } + 1,
        reason = reason,
    )
}

internal data class EntryViewerSettingsProjectionBoundarySource(
    val relativePath: String,
    val content: String,
)

internal data class EntryViewerSettingsProjectionBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int?,
    val reason: String,
)

private data class ViewerSettingsScreenDeclaration(
    val name: String,
    val source: EntryViewerSettingsProjectionBoundarySource,
    val offset: Int,
)

private const val VIEWER_SETTINGS_SCREEN_REGISTRY =
    "app/src/main/java/eu/kanade/presentation/more/settings/screen/EntryViewerSettingsScreenProjections.kt"
private const val APP_MODULE = "app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt"

private val APP_VIEWER_SETTINGS_SCREEN_DECLARATION = Regex(
    """\b(?:object|class)\s+([A-Za-z_][A-Za-z0-9_]*)\s*:\s*AppEntryViewerSettingsScreenProjection\b""",
)
private val VIEWER_SETTINGS_SCREEN_REFERENCE = Regex("""\b(Settings[A-Za-z0-9_]*Screen)\b""")
