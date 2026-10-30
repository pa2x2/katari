package mihon.gradle.platformparity

/*
 * Code lives where every platform can run it unless it needs one platform (§3.2 of the parity layout):
 * - A1: an Android class implementing a shared contract needs Android, or it belongs in shared code;
 * - A2: a binding in a platform-only source set names something only its platform has, or it belongs in a shared
 *   container;
 * - A3: an Android worker only adapts a shared runner to WorkManager.
 * Whether a file needs its platform is [PlatformBinding]'s.
 */

internal fun checkPlatformPlacement(
    sources: List<PlatformSource>,
    buildFiles: List<PlatformBuildFile>,
    apis: PlatformApis,
): List<PlatformPlacementFinding> {
    val codebase = PlatformCodebase(sources, buildFiles, apis)
    val binding = PlatformBinding(codebase)
    return (
        contractImplementationFindings(codebase, binding) +
            bindingPlacementFindings(codebase) +
            workerAdapterFindings(sources)
        )
        .sortedWith(compareBy({ it.relativePath }, { it.lineNumber }))
}
