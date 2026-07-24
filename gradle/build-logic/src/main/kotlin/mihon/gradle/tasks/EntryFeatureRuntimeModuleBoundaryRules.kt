package mihon.gradle.tasks

internal fun checkEntryFeatureRuntimeModuleBoundaries(
    sources: List<EntryFeatureRuntimeModuleBoundarySource>,
): List<EntryFeatureRuntimeModuleBoundaryFinding> {
    val findings = mutableListOf<EntryFeatureRuntimeModuleBoundaryFinding>()
    val productionSources = sources.filter { "/src/main/" in it.relativePath }
    val validationTopology = productionSources.singleOrNull {
        it.relativePath.endsWith("/runtime/EntryInteractionProductionGraphValidation.kt")
    }
    productionSources
        .filterNot { it.relativePath == validationTopology?.relativePath }
        .forEach { source ->
            source.content.lines().forEachIndexed { index, line ->
                if ("productionEntryFeatureGraphForValidation" in line) {
                    findings += EntryFeatureRuntimeModuleBoundaryFinding(
                        relativePath = source.relativePath,
                        lineNumber = index + 1,
                        reason = "graph-only production Feature view is validation-only; " +
                            "install runtime modules instead",
                    )
                }
            }
        }
    productionSources.forEach { source ->
        BEHAVIOR_ID_VALUE_REFERENCE.findAll(source.content).forEach { reference ->
            findings += EntryFeatureRuntimeModuleBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = source.lineNumber(reference.range.first),
                reason = "Feature Behavior projection ids are descriptive and cannot be used as runtime dispatch keys",
            )
        }
    }
    return findings
}

private fun EntryFeatureRuntimeModuleBoundarySource.lineNumber(offset: Int): Int {
    return content.take(offset).count { it == '\n' } + 1
}

internal data class EntryFeatureRuntimeModuleBoundarySource(
    val relativePath: String,
    val content: String,
)

internal data class EntryFeatureRuntimeModuleBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int?,
    val reason: String,
)

private val BEHAVIOR_ID_VALUE_REFERENCE = Regex(
    """\b[A-Za-z_][A-Za-z0-9_]*Behavior(?:\.[A-Z][A-Z0-9_]*)?\.id\.value\b""",
)
