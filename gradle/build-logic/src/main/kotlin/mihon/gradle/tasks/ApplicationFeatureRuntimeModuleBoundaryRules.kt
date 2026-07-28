package mihon.gradle.tasks

internal fun checkApplicationFeatureRuntimeModuleBoundaries(
    sources: List<ApplicationFeatureRuntimeModuleBoundarySource>,
): List<ApplicationFeatureRuntimeModuleBoundaryFinding> {
    val kotlinSources = sources.filter { it.relativePath.endsWith(".kt") && "/src/main/" in it.relativePath }
    val declarations = kotlinSources.flatMap { source ->
        val packageName = PACKAGE_DECLARATION.find(source.content)?.groupValues?.get(1)
            ?: return@flatMap emptyList()
        APPLICATION_FEATURE_MODULE_DECLARATION.findAll(source.content).map { match ->
            ApplicationFeatureRuntimeModuleDeclaration(
                symbol = "$packageName.${match.groupValues[1]}",
                moduleRoot = source.moduleRoot(),
                source = source,
                offset = match.range.first,
            )
        }.toList()
    }
    val componentDeclarations = kotlinSources.flatMap { source ->
        val packageName = PACKAGE_DECLARATION.find(source.content)?.groupValues?.get(1)
            ?: return@flatMap emptyList()
        APPLICATION_FEATURE_RUNTIME_COMPONENT_DECLARATION.findAll(source.content).map { match ->
            ApplicationFeatureRuntimeComponentDeclaration(
                symbol = "$packageName.${match.groupValues[1]}",
                moduleRoot = source.moduleRoot(),
                source = source,
                offset = match.range.first,
            )
        }.toList()
    }
    val descriptors = sources
        .filter { it.relativePath.endsWith(".application-feature-module") }
        .mapNotNull { source ->
            source.content.lineSequence()
                .map { it.substringBefore("#").trim() }
                .firstOrNull { it.startsWith("module=") }
                ?.substringAfter("=")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { symbol ->
                    ApplicationFeatureRuntimeModuleRegistration(
                        symbol = symbol,
                        moduleRoot = source.moduleRoot(),
                        source = source,
                    )
                }
        }
    val registrationsBySymbol = descriptors.groupBy(ApplicationFeatureRuntimeModuleRegistration::symbol)
    val declarationsBySymbol = declarations.groupBy(ApplicationFeatureRuntimeModuleDeclaration::symbol)
    val componentDescriptors = sources
        .filter { it.relativePath.endsWith(".application-feature-runtime-component") }
        .mapNotNull { source ->
            source.content.lineSequence()
                .map { it.substringBefore("#").trim() }
                .firstOrNull { it.startsWith("component=") }
                ?.substringAfter("=")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let { symbol ->
                    ApplicationFeatureRuntimeComponentRegistration(
                        symbol = symbol,
                        moduleRoot = source.moduleRoot(),
                        source = source,
                    )
                }
        }
    val componentRegistrationsBySymbol =
        componentDescriptors.groupBy(ApplicationFeatureRuntimeComponentRegistration::symbol)
    val componentDeclarationsBySymbol =
        componentDeclarations.groupBy(ApplicationFeatureRuntimeComponentDeclaration::symbol)
    val findings = mutableListOf<ApplicationFeatureRuntimeModuleBoundaryFinding>()

    declarations.forEach { declaration ->
        val registrations = registrationsBySymbol[declaration.symbol].orEmpty()
        if (registrations.isEmpty()) {
            findings += declaration.source.finding(
                declaration.offset,
                "Application Feature runtime module is missing its owner-local descriptor: ${declaration.symbol}",
            )
        } else if (registrations.none { it.moduleRoot == declaration.moduleRoot }) {
            findings += ApplicationFeatureRuntimeModuleBoundaryFinding(
                relativePath = registrations.first().source.relativePath,
                lineNumber = null,
                reason = "Application Feature descriptor for ${declaration.symbol} must live in " +
                    "${declaration.moduleRoot}",
            )
        }
    }
    descriptors.forEach { registration ->
        if (registration.symbol !in declarationsBySymbol) {
            findings += ApplicationFeatureRuntimeModuleBoundaryFinding(
                relativePath = registration.source.relativePath,
                lineNumber = null,
                reason = "Application Feature descriptor names no production runtime module: ${registration.symbol}",
            )
        }
    }
    componentDeclarations.forEach { declaration ->
        val registrations = componentRegistrationsBySymbol[declaration.symbol].orEmpty()
        if (registrations.isEmpty()) {
            findings += declaration.source.finding(
                declaration.offset,
                "Application Feature runtime component is missing its owner-local descriptor: ${declaration.symbol}",
            )
        } else if (registrations.none { it.moduleRoot == declaration.moduleRoot }) {
            findings += ApplicationFeatureRuntimeModuleBoundaryFinding(
                relativePath = registrations.first().source.relativePath,
                lineNumber = null,
                reason = "Application Feature runtime component descriptor for ${declaration.symbol} must live in " +
                    declaration.moduleRoot,
            )
        }
    }
    componentDescriptors.forEach { registration ->
        if (registration.symbol !in componentDeclarationsBySymbol) {
            findings += ApplicationFeatureRuntimeModuleBoundaryFinding(
                relativePath = registration.source.relativePath,
                lineNumber = null,
                reason = "Application Feature runtime component descriptor names no production component: " +
                    registration.symbol,
            )
        }
    }

    val runtimeInfrastructurePresent = kotlinSources.any { source ->
        APPLICATION_FEATURE_MODULE_CLASS.containsMatchIn(source.content)
    }
    sources.singleOrNull {
        runtimeInfrastructurePresent && it.relativePath == "app/build.gradle.kts"
    }?.let { appBuild ->
        if ("GenerateApplicationFeatureTopologyTask" !in appBuild.content ||
            ".application-feature-module" !in appBuild.content ||
            ".application-feature-runtime-component" !in appBuild.content
        ) {
            findings += ApplicationFeatureRuntimeModuleBoundaryFinding(
                relativePath = appBuild.relativePath,
                lineNumber = null,
                reason = "app must generate its Application Feature topology from owner-local descriptors",
            )
        }
    }
    return findings.distinct()
}

private fun ApplicationFeatureRuntimeModuleBoundarySource.moduleRoot(): String =
    relativePath.substringBefore("/src/")

private fun ApplicationFeatureRuntimeModuleBoundarySource.finding(
    offset: Int,
    reason: String,
): ApplicationFeatureRuntimeModuleBoundaryFinding {
    return ApplicationFeatureRuntimeModuleBoundaryFinding(
        relativePath = relativePath,
        lineNumber = content.take(offset).count { it == '\n' } + 1,
        reason = reason,
    )
}

internal data class ApplicationFeatureRuntimeModuleBoundarySource(
    val relativePath: String,
    val content: String,
)

internal data class ApplicationFeatureRuntimeModuleBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int?,
    val reason: String,
)

private data class ApplicationFeatureRuntimeModuleDeclaration(
    val symbol: String,
    val moduleRoot: String,
    val source: ApplicationFeatureRuntimeModuleBoundarySource,
    val offset: Int,
)

private data class ApplicationFeatureRuntimeModuleRegistration(
    val symbol: String,
    val moduleRoot: String,
    val source: ApplicationFeatureRuntimeModuleBoundarySource,
)

private data class ApplicationFeatureRuntimeComponentDeclaration(
    val symbol: String,
    val moduleRoot: String,
    val source: ApplicationFeatureRuntimeModuleBoundarySource,
    val offset: Int,
)

private data class ApplicationFeatureRuntimeComponentRegistration(
    val symbol: String,
    val moduleRoot: String,
    val source: ApplicationFeatureRuntimeModuleBoundarySource,
)

private val PACKAGE_DECLARATION = Regex("""\bpackage\s+([A-Za-z_][A-Za-z0-9_.]*)""")
private val APPLICATION_FEATURE_MODULE_DECLARATION = Regex(
    """(?m)^\s*(?:(?:internal|public)\s+)?val\s+([A-Za-z_][A-Za-z0-9_]*)""" +
        """(?:\s*:\s*ApplicationFeatureRuntimeModule)?\s*=\s*ApplicationFeatureRuntimeModule\s*\(""",
)
private val APPLICATION_FEATURE_MODULE_CLASS = Regex("""\bclass\s+ApplicationFeatureRuntimeModule\s*\(""")
private val APPLICATION_FEATURE_RUNTIME_COMPONENT_DECLARATION = Regex(
    """(?m)^\s*(?:(?:internal|public)\s+)?val\s+([A-Za-z_][A-Za-z0-9_]*)""" +
        """\s*:\s*ApplicationFeatureRuntimeComponent\s*=""",
)
