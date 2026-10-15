package mihon.gradle.tasks

internal fun checkFeatureBehaviorIdBoundaries(
    sources: List<FeatureBehaviorIdBoundarySource>,
): List<FeatureBehaviorIdBoundaryFinding> {
    val findings = mutableListOf<FeatureBehaviorIdBoundaryFinding>()
    val productionSources = sources.filter { "/src/main/" in it.relativePath }
    productionSources.forEach { source ->
        BEHAVIOR_ID_VALUE_REFERENCE.findAll(source.content).forEach { reference ->
            findings += FeatureBehaviorIdBoundaryFinding(
                relativePath = source.relativePath,
                lineNumber = source.lineNumber(reference.range.first),
                reason = "Feature Behavior projection ids are descriptive and cannot be used as runtime dispatch keys",
            )
        }
    }
    return findings
}

private fun FeatureBehaviorIdBoundarySource.lineNumber(offset: Int): Int {
    return content.take(offset).count { it == '\n' } + 1
}

internal data class FeatureBehaviorIdBoundarySource(
    val relativePath: String,
    val content: String,
)

internal data class FeatureBehaviorIdBoundaryFinding(
    val relativePath: String,
    val lineNumber: Int?,
    val reason: String,
)

private val BEHAVIOR_ID_VALUE_REFERENCE = Regex(
    """\b[A-Za-z_][A-Za-z0-9_]*Behavior(?:\.[A-Z][A-Z0-9_]*)?\.id\.value\b""",
)
