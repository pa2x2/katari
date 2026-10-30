package mihon.gradle.platformparity

/*
 * A1. An Android class that implements a shared contract (an interface or abstract class of `commonMain`) answers that
 * contract for Android. If its file needs nothing Android has and desktop lacks, the answer is the same on desktop,
 * and leaving it Android-only is how desktop came to lack it or to copy it. Only the modules the Android app ships
 * count, so test apps and the showcase extension are out. Platform facilities are each platform's own by declaration,
 * so a file implementing one is bound to Android by definition, as [PlatformBinding] counts it.
 */

internal fun contractImplementationFindings(
    codebase: PlatformCodebase,
    binding: PlatformBinding,
): List<PlatformPlacementFinding> {
    return codebase.files
        .filter { it.platform == Platform.ANDROID && it.sourceSet.module.path in codebase.androidAppModules }
        .filterNot(binding::isBound)
        .flatMap { file ->
            file.types.filter { "actual" !in it.modifiers && it.kind != TypeKind.INTERFACE }.mapNotNull { type ->
                val contract = codebase.sharedSupertypes(file, type)
                    .firstOrNull { (_, declaration) ->
                        declaration.isContract() && declaration.qualifiedName !in codebase.facilities
                    }
                    ?: return@mapNotNull null
                val name = type.name ?: "An object"
                PlatformPlacementFinding(
                    relativePath = file.relativePath,
                    lineNumber = file.lineNumberAt(type.offset),
                    reason = "$name implements the shared ${contract.second.qualifiedName} but needs nothing only " +
                        "Android has; move ${file.relativePath.substringAfterLast('/')} to commonMain",
                )
            }
        }
}

private fun TypeDeclaration.isContract(): Boolean =
    kind == TypeKind.INTERFACE || (kind == TypeKind.CLASS && ("abstract" in modifiers || "sealed" in modifiers))
