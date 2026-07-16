package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class EntryInteractionBoundaryCheckTask : DefaultTask() {

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @TaskAction
    fun action() {
        val root = repositoryRoot.get().asFile
        val typeModules = TypeModule.discover(root)
        val sourceIndex = KotlinSourceIndex.create(root, sourceFiles(root, typeModules))
        val findings = EntryInteractionBoundaryRules(root, sourceIndex, typeModules).check()

        if (findings.isNotEmpty()) {
            val maxFindings = 80
            val rendered = findings
                .take(maxFindings)
                .joinToString(separator = "\n") { finding ->
                    val location = if (finding.lineNumber != null) {
                        "${finding.relativePath}:${finding.lineNumber}"
                    } else {
                        finding.relativePath
                    }
                    "- $location: ${finding.reason}"
                }
            val suffix = if (findings.size > maxFindings) {
                "\n- ... ${findings.size - maxFindings} more finding(s)"
            } else {
                ""
            }
            throw GradleException("Entry interaction boundary check failed:\n$rendered$suffix")
        }
    }

    private fun sourceFiles(root: File, typeModules: List<TypeModule>): List<File> {
        return SOURCE_ROOTS
            .map { root.resolve(it) }
            .plus(typeModules.map { it.sourceRoot })
            .filter { it.isDirectory }
            .flatMap { directory ->
                directory.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filterNot { it.invariantSeparatorsPath.contains("/build/") }
                    .toList()
            }
    }

    companion object {
        private val SOURCE_ROOTS = listOf(
            "app/src/main/java",
            "data/src/main/java",
            "domain/src/main/java",
            "presentation-core/src/main/java",
            "presentation-widget/src/main/java",
            "source-local/src/main/java",
            "source-compat/src/main/java",
            "entry-interactions/src/main/java",
            "entry-interactions/api/src/main/java",
            "entry-interactions/spi/src/main/java",
        )
    }
}

private class EntryInteractionBoundaryRules(
    private val root: File,
    private val sourceIndex: KotlinSourceIndex,
    private val typeModules: List<TypeModule>,
) {
    private val processorInterfaceNames = sourceIndex.files
        .firstOrNull {
            it.relativePath == "entry-interactions/spi/src/main/java/mihon/entry/interactions/EntryInteractionPlugin.kt"
        }
        ?.topLevelDeclarations
        ?.filter { it.kind == KotlinDeclarationKind.INTERFACE }
        ?.map { it.name }
        ?.filter { it.startsWith("Entry") && it.endsWith("Processor") }
        ?.toSet()
        .orEmpty()

    private val internalApiNames = processorInterfaceNames + setOf(
        "EntryInteractionPlugin",
        "EntryInteractionRegistry",
        "createEntryInteractions",
    )
    private val processorImplementations = sourceIndex.files
        .asSequence()
        .filter { it.owningTypeModule() != null }
        .flatMap { file ->
            file.topLevelDeclarations
                .filter { declaration -> declaration.superTypeNames.any { it in processorInterfaceNames } }
                .map { declaration ->
                    DerivedSymbol(
                        simpleName = declaration.name,
                        qualifiedName = declaration.qualifiedName,
                        owner = file.owningTypeModule()!!,
                        declaration = declaration,
                    )
                }
        }
        .toList()

    private val runtimeEntryPointSymbols = sourceIndex.files
        .asSequence()
        .filter { it.owningTypeModule() != null }
        .flatMap { file ->
            file.topLevelDeclarations
                .filter { it.name == "ReaderActivity" || it.name == "VideoPlayerActivity" }
                .map {
                    DerivedSymbol(
                        simpleName = it.name,
                        qualifiedName = it.qualifiedName,
                        owner = file.owningTypeModule()!!,
                        declaration = it,
                    )
                }
        }
        .toList()

    private val runtimeInternalPackages = runtimeEntryPointSymbols
        .map { symbol ->
            RuntimeInternalPackage(
                packageName = symbol.qualifiedName.substringBeforeLast("."),
                owner = symbol.owner,
            )
        }

    private val runtimeInternalSymbols = sourceIndex.files
        .asSequence()
        .mapNotNull { file ->
            val owner = file.owningTypeModule() ?: return@mapNotNull null
            val runtimePackage = runtimeInternalPackages.firstOrNull { runtimePackage ->
                runtimePackage.owner == owner &&
                    (
                        file.packageName == runtimePackage.packageName ||
                            file.packageName.startsWith("${runtimePackage.packageName}.")
                        )
            } ?: return@mapNotNull null
            file to runtimePackage
        }
        .flatMap { (file, runtimePackage) ->
            file.topLevelDeclarations
                .filter { it.kind != KotlinDeclarationKind.FUNCTION }
                .filter { it.isRuntimeMediaResolutionInternal() }
                .map {
                    DerivedSymbol(
                        simpleName = it.name,
                        qualifiedName = it.qualifiedName,
                        owner = runtimePackage.owner,
                        declaration = it,
                    )
                }
        }
        .distinctBy { it.qualifiedName }
        .toList()

    private val downloadRuntimeSymbols = sourceIndex.files
        .asSequence()
        .filter { file ->
            file.owningTypeModule() != null &&
                file.packageName.startsWith("${file.owningTypeModule()!!.packagePrefix}.download")
        }
        .flatMap { file ->
            file.topLevelDeclarations
                .filter { it.kind != KotlinDeclarationKind.FUNCTION }
                .filter { it.isPublic }
                .filter { it.name.firstOrNull()?.isUpperCase() == true }
                .map {
                    DerivedSymbol(
                        simpleName = it.name,
                        qualifiedName = it.qualifiedName,
                        owner = file.owningTypeModule()!!,
                        declaration = it,
                    )
                }
        }
        .distinctBy { it.qualifiedName }
        .toList()

    fun check(): List<Finding> {
        val findings = mutableListOf<Finding>()

        checkAppGradleDependencies(findings)
        checkTypeModuleGradleDependencies(findings)

        sourceIndex.files.forEach { file ->
            checkForbiddenImports(file, findings)
            checkRootCompositionTypeModuleImports(file, findings)
            checkForbiddenAppReferences(file, findings)
            checkLegacyInteractionApis(file, findings)
            checkInternalApiReferences(file, findings)
            checkProcessorImplementationReferences(file, findings)
            checkRuntimeEntryPointReferences(file, findings)
            checkRuntimeInternalReferences(file, findings)
            checkDownloadRuntimeReferences(file, findings)
            checkSourceMediaResolutionReferences(file, findings)
            checkMediaCacheMaintenanceReferences(file, findings)
            checkExhaustiveEntryTypeMappings(file, findings)
            checkSuspiciousTypeBranches(file, findings)
        }

        checkTypeModulePublicApis(findings)

        return findings
    }

    private fun checkAppGradleDependencies(findings: MutableList<Finding>) {
        val file = root.resolve("app/build.gradle.kts")
        if (!file.isFile) return

        val forbiddenDependencies = typeModules
            .map { "projects.entryInteractions.${it.gradleAccessor}" }
            .plus("projects.entryInteractions.api")
            .plus("projects.entryInteractions.spi")

        file.readLines().forEachIndexed { index, line ->
            forbiddenDependencies.forEach { dependency ->
                if (line.contains(dependency)) {
                    findings += Finding(
                        relativePath = "app/build.gradle.kts",
                        lineNumber = index + 1,
                        reason = "app must depend only on projects.entryInteractions for Entry interactions: " +
                            dependency,
                    )
                }
            }
        }
    }

    private fun checkTypeModuleGradleDependencies(findings: MutableList<Finding>) {
        typeModules.forEach { module ->
            val file = module.gradleFile
            if (!file.isFile) return@forEach

            file.readLines().forEachIndexed { index, line ->
                if (line.contains("testImplementation")) return@forEachIndexed
                if (line.contains("projects.entryInteractions)") || line.contains("projects.entryInteractions,")) {
                    findings += Finding(
                        relativePath = module.gradleRelativePath,
                        lineNumber = index + 1,
                        reason = "type interaction modules must depend on projects.entryInteractions.spi, not root",
                    )
                }
            }
        }
    }

    private fun checkForbiddenImports(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (!file.isStrictImportCheckedPath()) return

        file.imports.forEach { import ->
            typeModules.forEach { module ->
                if (import.startsWith(module.packagePrefix)) {
                    findings += Finding(
                        relativePath = file.relativePath,
                        lineNumber = import.lineNumber,
                        reason = "direct import across Entry interaction boundary: ${module.packagePrefix}.",
                    )
                }
            }

            runtimeInternalPackages.forEach { runtimePackage ->
                if (import.startsWith(runtimePackage.packageName)) {
                    findings += Finding(
                        relativePath = file.relativePath,
                        lineNumber = import.lineNumber,
                        reason = "direct import of Entry runtime internals: ${runtimePackage.packageName}.",
                    )
                }
            }
        }
    }

    private fun checkRootCompositionTypeModuleImports(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (!file.relativePath.startsWith("entry-interactions/src/main/")) return

        file.imports.forEach { import ->
            val importedFqName = import.importedFqName ?: return@forEach
            val module = typeModules.firstOrNull { importedFqName.startsWith("${it.packagePrefix}.") }
                ?: return@forEach
            if (importedFqName in ROOT_TYPE_MODULE_IMPORT_ALLOWLIST) return@forEach

            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = import.lineNumber,
                reason = "root Entry interaction composition may import only public ${module.name} installer/plugin " +
                    "bridges, not type-module implementation symbols: $importedFqName",
            )
        }
    }

    private fun checkForbiddenAppReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (!file.relativePath.startsWith("app/src/main/")) return

        file.findReference("FilterEntryChaptersForDownload")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "app must route auto-download filtering through EntryDownloadInteraction, not " +
                    "FilterEntryChaptersForDownload",
            )
        }
    }

    private fun checkLegacyInteractionApis(file: KotlinSourceFile, findings: MutableList<Finding>) {
        LEGACY_INTERACTION_APIS.forEach { api ->
            file.references.firstOrNull { it.name == api }?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "legacy Entry interaction API usage: $api",
                )
            }
        }
    }

    private fun checkInternalApiReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isRootOrTypeModuleOrTestPath()) return

        internalApiNames.forEach { name ->
            file.findReference(name)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "$name is root/type-module Entry interaction internals",
                )
            }
        }
    }

    private fun checkProcessorImplementationReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        processorImplementations.forEach { symbol ->
            if (file.isOwnedBy(symbol.owner) || file.isTestPath()) return@forEach

            file.findReference(symbol)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason =
                    "direct concrete ${symbol.owner.name} processor reference outside owning type module/tests: " +
                        symbol.simpleName,
                )
            }
        }
    }

    private fun checkRuntimeEntryPointReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        runtimeEntryPointSymbols.forEach { symbol ->
            if (file.isOwnedBy(symbol.owner) || file.isTestPath()) return@forEach

            file.findReference(symbol)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "direct ${symbol.simpleName} reference outside owning type module launch API",
                )
            }
        }
    }

    private fun checkRuntimeInternalReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        runtimeInternalSymbols.forEach { symbol ->
            if (file.isOwnedBy(symbol.owner) || file.isTestPath()) return@forEach

            file.findReference(symbol)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "direct ${symbol.simpleName} runtime media resolution reference outside " +
                        "${symbol.owner.name} type module",
                )
            }
        }
    }

    private fun checkDownloadRuntimeReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isRootCompositionPath() || file.isTestPath()) return

        downloadRuntimeSymbols.forEach { symbol ->
            if (file.isOwnedBy(symbol.owner)) return@forEach

            file.findReference(symbol)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "direct ${symbol.simpleName} download runtime reference outside " +
                        "${symbol.owner.name} type module/root composition",
                )
            }
        }
    }

    private fun checkSourceMediaResolutionReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (!file.isSourceMediaResolutionGuardedPath()) return

        file.imports
            .firstOrNull { it.importedFqName == LEGACY_MANGA_PAGE_FQ_NAME }
            ?.let { import ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = import.lineNumber,
                    reason = "generic code must route manga page resolution through Entry source/media APIs, " +
                        "not legacy Page",
                )
            }

        file.findReference("getPageList")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "generic code must route manga page resolution through Entry source/media APIs, " +
                    "not legacy getPageList",
            )
        }
    }

    private fun checkMediaCacheMaintenanceReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (!file.isMediaCacheMaintenanceGuardedPath()) return

        MEDIA_CACHE_IMPLEMENTATION_TYPES.forEach { typeName ->
            file.findReference(typeName)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "settings/UI cache maintenance must use EntryMediaCacheMaintenance and " +
                        "EntryMediaCacheBucket keys, not concrete cache implementation: $typeName",
                )
            }
        }
    }

    private fun checkExhaustiveEntryTypeMappings(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isExhaustiveEntryTypeMappingAllowedPath()) return

        val mangaLine = file.lineNumberOf("EntryType.MANGA ->")
        val animeLine = file.lineNumberOf("EntryType.ANIME ->")
        if (mangaLine == null || animeLine == null) return

        findings += Finding(
            relativePath = file.relativePath,
            lineNumber = minOf(mangaLine, animeLine),
            reason = "generic EntryType MANGA/ANIME mapping must use EntryTypePresentation or an approved " +
                "compatibility/storage boundary",
        )
    }

    private fun checkSuspiciousTypeBranches(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTypeBranchAllowedPath()) return
        if (!TYPE_BRANCH_PATTERNS.any { it.containsMatchIn(file.content) }) return

        val suspiciousNames = processorImplementations.map { it.simpleName }.toSet() +
            runtimeEntryPointSymbols.map { it.simpleName } +
            runtimeInternalSymbols.map { it.simpleName } +
            downloadRuntimeSymbols.map { it.simpleName } +
            LEGACY_INTERACTION_APIS

        if (file.references.none { it.name in suspiciousNames }) return

        val lineNumber = file.content.lines()
            .indexOfFirst { line -> TYPE_BRANCH_PATTERNS.any { it.containsMatchIn(line) } }
            .takeIf { it >= 0 }
            ?.plus(1)

        findings += Finding(
            relativePath = file.relativePath,
            lineNumber = lineNumber,
            reason = "suspicious EntryType processing branch reaches across Entry interaction boundaries",
        )
    }

    private fun checkTypeModulePublicApis(findings: MutableList<Finding>) {
        sourceIndex.files
            .filter { it.owningTypeModule() != null }
            .forEach { file ->
                file.topLevelDeclarations
                    .filter { it.isPublic }
                    .forEach { declaration ->
                        if (
                            declaration.kind == KotlinDeclarationKind.FUNCTION &&
                            !file.relativePath.endsWith("Launch.kt")
                        ) {
                            return@forEach
                        }
                        if (declaration.isProcessorImplementation()) {
                            findings += Finding(
                                relativePath = file.relativePath,
                                lineNumber = declaration.lineNumber,
                                reason = "type-module processor must remain internal: ${declaration.name}",
                            )
                            return@forEach
                        }

                        if (file.isPublicTypeModuleApiPath()) return@forEach
                        if (declaration.isDependencyContainer()) return@forEach
                        if (declaration.isPluginFactory()) return@forEach
                        if (declaration.isLibraryProgressCalculatorFactory()) return@forEach
                        if (declaration.isRuntimeBridgeFunction(file)) return@forEach

                        findings += Finding(
                            relativePath = file.relativePath,
                            lineNumber = declaration.lineNumber,
                            reason = "unexpected public type-module ${declaration.kind.label}: ${declaration.name}",
                        )
                    }
            }
    }

    private fun KotlinDeclaration.isProcessorImplementation(): Boolean {
        return superTypeNames.any { it in processorInterfaceNames }
    }

    private fun KotlinDeclaration.isDependencyContainer(): Boolean {
        return kind == KotlinDeclarationKind.CLASS &&
            name.endsWith("EntryInteractionDependencies")
    }

    private fun KotlinDeclaration.isPluginFactory(): Boolean {
        return kind == KotlinDeclarationKind.FUNCTION &&
            returnTypeName == "EntryInteractionPlugin"
    }

    private fun KotlinDeclaration.isLibraryProgressCalculatorFactory(): Boolean {
        return kind == KotlinDeclarationKind.FUNCTION &&
            returnTypeName == "EntryLibraryProgressCalculator"
    }

    private fun KotlinDeclaration.isRuntimeBridgeFunction(file: KotlinSourceFile): Boolean {
        if (kind != KotlinDeclarationKind.FUNCTION) return false
        return file.relativePath.endsWith("RuntimeModule.kt") ||
            file.relativePath.endsWith("MangaReaderCoilComponents.kt")
    }

    private fun KotlinDeclaration.isRuntimeMediaResolutionInternal(): Boolean {
        return name.firstOrNull()?.isUpperCase() == true &&
            RUNTIME_MEDIA_RESOLUTION_INTERNAL_NAME_PATTERNS.any { it.containsMatchIn(name) }
    }

    private fun KotlinSourceFile.findReference(symbol: DerivedSymbol): KotlinReference? {
        return imports.firstOrNull { it.importedFqName == symbol.qualifiedName }
            ?.let { KotlinReference(symbol.simpleName, it.lineNumber) }
            ?: references.firstOrNull { it.name == symbol.simpleName }
    }

    private fun KotlinSourceFile.findReference(simpleName: String): KotlinReference? {
        return imports.firstOrNull { it.importedFqName?.substringAfterLast(".") == simpleName }
            ?.let { KotlinReference(simpleName, it.lineNumber) }
            ?: references.firstOrNull { it.name == simpleName }
    }

    private fun KotlinSourceFile.isOwnedBy(module: TypeModule?): Boolean {
        return module != null && relativePath.startsWith("${module.relativePath}/")
    }

    private fun KotlinSourceFile.owningTypeModule(): TypeModule? {
        return typeModules.firstOrNull { relativePath.startsWith("${it.relativePath}/") }
    }

    private fun KotlinSourceFile.isRootOrTypeModuleOrTestPath(): Boolean {
        return relativePath.startsWith("entry-interactions/src/main/") ||
            relativePath.startsWith("entry-interactions/api/src/main/") ||
            relativePath.startsWith("entry-interactions/spi/src/main/") ||
            owningTypeModule() != null ||
            isTestPath()
    }

    private fun KotlinSourceFile.isRootCompositionPath(): Boolean {
        return relativePath == "entry-interactions/src/main/java/mihon/entry/interactions/EntryInteractionRuntime.kt"
    }

    private fun KotlinSourceFile.isSourceMediaResolutionGuardedPath(): Boolean {
        if (isRootOrTypeModuleOrTestPath()) return false
        if (!SOURCE_MEDIA_RESOLUTION_GUARDED_ROOTS.any { relativePath.startsWith(it) }) return false
        if (relativePath in SOURCE_MEDIA_RESOLUTION_ALLOWED_FILES) return false
        return SOURCE_MEDIA_RESOLUTION_ALLOWED_PREFIXES.none { relativePath.startsWith(it) }
    }

    private fun KotlinSourceFile.isMediaCacheMaintenanceGuardedPath(): Boolean {
        if (isRootOrTypeModuleOrTestPath()) return false
        return MEDIA_CACHE_MAINTENANCE_GUARDED_ROOTS.any { relativePath.startsWith(it) }
    }

    private fun KotlinSourceFile.isExhaustiveEntryTypeMappingAllowedPath(): Boolean {
        return relativePath.startsWith("entry-interactions/") ||
            isTestPath() ||
            relativePath == "app/src/main/java/eu/kanade/presentation/entry/EntryTypePresentation.kt" ||
            relativePath == "app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt" ||
            relativePath.startsWith("app/src/main/java/eu/kanade/tachiyomi/data/backup/") ||
            relativePath.startsWith("app/src/main/java/eu/kanade/tachiyomi/data/track/") ||
            relativePath.startsWith("app/src/main/java/eu/kanade/tachiyomi/source/") ||
            relativePath.startsWith("app/src/main/java/eu/kanade/domain/source/") ||
            relativePath.startsWith("app/src/main/java/mihon/core/migration/") ||
            relativePath.startsWith("app/src/main/java/mihon/feature/migration/") ||
            relativePath.startsWith("source-local/") ||
            relativePath.startsWith("source-compat/")
    }

    private fun KotlinSourceFile.isStrictImportCheckedPath(): Boolean {
        if (isTestPath()) return false
        return STRICT_IMPORT_CHECKED_ROOTS.any { relativePath.startsWith(it) }
    }

    private fun KotlinSourceFile.isTypeBranchAllowedPath(): Boolean {
        return relativePath.startsWith("entry-interactions/") ||
            isTestPath() ||
            relativePath == "app/src/main/java/eu/kanade/tachiyomi/di/AppModule.kt" ||
            relativePath.startsWith("app/src/main/java/eu/kanade/presentation/") ||
            relativePath.startsWith("app/src/main/java/eu/kanade/tachiyomi/data/backup/") ||
            relativePath.startsWith("app/src/main/java/eu/kanade/tachiyomi/source/") ||
            relativePath.startsWith("app/src/main/java/mihon/core/migration/") ||
            relativePath.startsWith("app/src/main/java/mihon/feature/migration/")
    }

    private fun KotlinSourceFile.isPublicTypeModuleApiPath(): Boolean {
        return relativePath in PUBLIC_TYPE_MODULE_ANDROID_COMPONENT_FILES
    }

    private fun KotlinSourceFile.isTestPath(): Boolean {
        return relativePath.contains("/src/test/")
    }

    private fun KotlinImport.startsWith(packagePrefix: String): Boolean {
        return importedFqName == packagePrefix ||
            importedFqName?.startsWith("$packagePrefix.") == true
    }

    companion object {
        private val STRICT_IMPORT_CHECKED_ROOTS = listOf(
            "app/src/main/",
            "data/src/main/",
            "domain/src/main/",
            "presentation-core/src/main/",
            "presentation-widget/src/main/",
            "source-local/src/main/",
            "source-compat/src/main/",
        )

        private val LEGACY_INTERACTION_APIS = listOf(
            "openEntryHandlerFor",
            "continueEntryHandlerFor",
            "downloadEntryHandlerFor",
            "EntryDownloader",
        )

        private const val LEGACY_MANGA_PAGE_FQ_NAME = "eu.kanade.tachiyomi.source.model.Page"

        private val RUNTIME_MEDIA_RESOLUTION_INTERNAL_NAME_PATTERNS = listOf(
            Regex("""Resolver$"""),
            Regex("""^Resolve"""),
            Regex("""PageLoader$"""),
            Regex("""PageCache$"""),
            Regex("""MediaCache$"""),
            Regex("""^VideoPlaybackSession$"""),
        )

        private val SOURCE_MEDIA_RESOLUTION_GUARDED_ROOTS = listOf(
            "app/src/main/java/",
            "data/src/main/java/",
            "domain/src/main/java/",
            "presentation-core/src/main/java/",
            "presentation-widget/src/main/java/",
        )

        private val SOURCE_MEDIA_RESOLUTION_ALLOWED_PREFIXES = listOf(
            "app/src/main/java/eu/kanade/tachiyomi/source/",
            "domain/src/main/java/tachiyomi/domain/source/",
        )

        private val SOURCE_MEDIA_RESOLUTION_ALLOWED_FILES = setOf(
            "app/src/main/java/eu/kanade/tachiyomi/data/cache/MangaPageCache.kt",
        )

        private val MEDIA_CACHE_IMPLEMENTATION_TYPES = setOf(
            "MangaPageCache",
            "AppMangaPageImageCache",
            "ReaderPageCache",
            "VideoPlayerMediaCache",
        )

        private val MEDIA_CACHE_MAINTENANCE_GUARDED_ROOTS = listOf(
            "app/src/main/java/eu/kanade/presentation/",
            "app/src/main/java/eu/kanade/tachiyomi/ui/",
            "presentation-core/src/main/java/",
            "presentation-widget/src/main/java/",
        )

        private val TYPE_BRANCH_PATTERNS = listOf(
            Regex("""\bEntryType\.[A-Z_]+\b"""),
            Regex("""\bentry\.type\b"""),
            Regex("""\bentryType\b"""),
        )

        private val ROOT_TYPE_MODULE_IMPORT_ALLOWLIST = setOf(
            "mihon.entry.interactions.anime.AnimeEntryInteractionDependencies",
            "mihon.entry.interactions.anime.addAnimeEntryInteractionRuntime",
            "mihon.entry.interactions.anime.animeEntryInteractionPlugin",
            "mihon.entry.interactions.anime.animeEntryLibraryProgressCalculator",
            "mihon.entry.interactions.book.BookEntryInteractionDependencies",
            "mihon.entry.interactions.book.addBookEntryInteractionRuntime",
            "mihon.entry.interactions.book.bookEntryInteractionPlugin",
            "mihon.entry.interactions.book.bookEntryLibraryProgressCalculator",
            "mihon.entry.interactions.manga.MangaEntryInteractionDependencies",
            "mihon.entry.interactions.manga.addMangaEntryInteractionRuntime",
            "mihon.entry.interactions.manga.mangaEntryInteractionPlugin",
            "mihon.entry.interactions.manga.mangaEntryLibraryProgressCalculator",
            "mihon.entry.interactions.manga.reader.addMangaReaderImageComponents",
        )

        private val PUBLIC_TYPE_MODULE_ANDROID_COMPONENT_FILES = setOf(
            "entry-interactions/anime/src/main/java/eu/kanade/tachiyomi/ui/video/player/VideoPlayerActivity.kt",
            "entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt",
            "entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderNavigationOverlayView.kt",
            "entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/ReaderButton.kt",
            "entry-interactions/manga/src/main/java/mihon/entry/interactions/manga/download/DownloadJob.kt",
        )
    }
}

private class KotlinSourceIndex private constructor(
    val files: List<KotlinSourceFile>,
) {
    companion object {
        fun create(root: File, sourceFiles: List<File>): KotlinSourceIndex {
            val indexedFiles = sourceFiles.map { file ->
                val content = file.readText()
                KotlinSourceFile.from(
                    relativePath = file.relativeTo(root).invariantSeparatorsPath,
                    content = content,
                )
            }
            return KotlinSourceIndex(indexedFiles)
        }
    }
}

private data class KotlinSourceFile(
    val relativePath: String,
    val content: String,
    val packageName: String,
    val imports: List<KotlinImport>,
    val topLevelDeclarations: List<KotlinDeclaration>,
    val references: List<KotlinReference>,
) {
    private val lineStarts: List<Int> = content.lineStarts()

    fun lineNumberOf(pattern: String): Int? {
        return content.lines()
            .indexOfFirst { it.contains(pattern) }
            .takeIf { it >= 0 }
            ?.plus(1)
    }

    companion object {
        fun from(relativePath: String, content: String): KotlinSourceFile {
            val lineStarts = content.lineStarts()
            val tokens = content.kotlinTokens(lineStarts)
            val packageName = tokens.packageName(content)
            return KotlinSourceFile(
                relativePath = relativePath,
                content = content,
                packageName = packageName,
                imports = content.imports(),
                topLevelDeclarations = tokens.topLevelDeclarations(packageName),
                references = tokens
                    .filter { it.type == KotlinTokenType.IDENTIFIER }
                    .map { KotlinReference(it.text, it.lineNumber) },
            )
        }
    }
}

private data class KotlinImport(
    val importedFqName: String?,
    val lineNumber: Int,
)

private data class KotlinDeclaration(
    val name: String,
    val qualifiedName: String,
    val kind: KotlinDeclarationKind,
    val isPublic: Boolean,
    val superTypeNames: Set<String>,
    val returnTypeName: String?,
    val lineNumber: Int,
)

private enum class KotlinDeclarationKind(
    val label: String,
) {
    CLASS("class"),
    INTERFACE("interface"),
    OBJECT("object"),
    FUNCTION("function"),
    TYPE_ALIAS("type alias"),
}

private data class KotlinReference(
    val name: String,
    val lineNumber: Int,
)

private data class KotlinToken(
    val type: KotlinTokenType,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val lineNumber: Int,
)

private enum class KotlinTokenType {
    IDENTIFIER,
    PACKAGE_KEYWORD,
    CLASS_KEYWORD,
    INTERFACE_KEYWORD,
    OBJECT_KEYWORD,
    FUN_KEYWORD,
    TYPE_ALIAS_KEYWORD,
    WHERE_KEYWORD,
    LBRACE,
    RBRACE,
    LPAR,
    RPAR,
    COLON,
    COLONCOLON,
    EQ,
    EOL_OR_SEMICOLON,
    WHITE_SPACE,
    OTHER,
}

private data class DerivedSymbol(
    val simpleName: String,
    val qualifiedName: String,
    val owner: TypeModule,
    val declaration: KotlinDeclaration,
)

private data class RuntimeInternalPackage(
    val packageName: String,
    val owner: TypeModule,
)

private data class Finding(
    val relativePath: String,
    val lineNumber: Int?,
    val reason: String,
)

private data class TypeModule(
    val name: String,
    val relativePath: String,
    val sourceRoot: File,
    val gradleFile: File,
) {
    val packagePrefix: String = "mihon.entry.interactions.$name"
    val gradleAccessor: String = name.toGradleAccessor()
    val gradleRelativePath: String = "$relativePath/build.gradle.kts"

    companion object {
        fun discover(root: File): List<TypeModule> {
            val entryInteractions = root.resolve("entry-interactions")
            return entryInteractions.listFiles()
                .orEmpty()
                .filter { it.isDirectory }
                .filter { it.name !in INFRASTRUCTURE_MODULES }
                .mapNotNull { directory ->
                    val sourceRoot = directory.resolve("src/main/java")
                    if (!sourceRoot.isDirectory) return@mapNotNull null
                    TypeModule(
                        name = directory.name,
                        relativePath = "entry-interactions/${directory.name}",
                        sourceRoot = sourceRoot,
                        gradleFile = directory.resolve("build.gradle.kts"),
                    )
                }
                .sortedBy { it.name }
        }
    }
}

private val INFRASTRUCTURE_MODULES = setOf("api", "spi", "download-notification")

private fun KotlinImport.startsWith(packagePrefix: String): Boolean {
    return importedFqName == packagePrefix ||
        importedFqName?.startsWith("$packagePrefix.") == true
}

private fun String.kotlinTokens(lineStarts: List<Int>): List<KotlinToken> {
    val tokens = mutableListOf<KotlinToken>()
    var offset = 0

    fun addToken(
        type: KotlinTokenType,
        startOffset: Int,
        endOffset: Int,
        text: String = substring(
            startOffset,
            endOffset,
        ),
    ) {
        tokens += KotlinToken(
            type = type,
            text = text,
            startOffset = startOffset,
            endOffset = endOffset,
            lineNumber = lineStarts.lineNumberFor(startOffset),
        )
    }

    while (offset < length) {
        val startOffset = offset
        when {
            this[offset] == ' ' || this[offset] == '\t' || this[offset] == '\r' -> {
                offset += 1
                while (offset < length && (this[offset] == ' ' || this[offset] == '\t' || this[offset] == '\r')) {
                    offset += 1
                }
                addToken(KotlinTokenType.WHITE_SPACE, startOffset, offset)
            }
            this[offset] == '\n' || this[offset] == ';' -> {
                offset += 1
                addToken(KotlinTokenType.EOL_OR_SEMICOLON, startOffset, offset)
            }
            startsWith("//", offset) -> {
                offset = indexOf('\n', offset).takeIf { it >= 0 } ?: length
            }
            startsWith("/*", offset) -> {
                offset = commentEndOffset(offset)
            }
            startsWith("\"\"\"", offset) -> {
                offset = rawStringEndOffset(offset)
            }
            this[offset] == '"' || this[offset] == '\'' -> {
                offset = quotedLiteralEndOffset(offset, this[offset])
            }
            this[offset] == '`' -> {
                offset = indexOf('`', offset + 1).takeIf { it >= 0 }?.plus(1) ?: length
                addToken(
                    KotlinTokenType.IDENTIFIER,
                    startOffset,
                    offset,
                    substring(startOffset + 1, (offset - 1).coerceAtLeast(startOffset + 1)),
                )
            }
            this[offset].isJavaIdentifierStart() -> {
                offset += 1
                while (offset < length && this[offset].isJavaIdentifierPart()) offset += 1
                val text = substring(startOffset, offset)
                addToken(text.kotlinTokenType(), startOffset, offset)
            }
            startsWith("::", offset) -> {
                offset += 2
                addToken(KotlinTokenType.COLONCOLON, startOffset, offset)
            }
            else -> {
                offset += 1
                val type = when (this[startOffset]) {
                    '{' -> KotlinTokenType.LBRACE
                    '}' -> KotlinTokenType.RBRACE
                    '(' -> KotlinTokenType.LPAR
                    ')' -> KotlinTokenType.RPAR
                    ':' -> KotlinTokenType.COLON
                    '=' -> KotlinTokenType.EQ
                    else -> KotlinTokenType.OTHER
                }
                addToken(type, startOffset, offset)
            }
        }
    }
    return tokens
}

private fun String.commentEndOffset(startOffset: Int): Int {
    var offset = startOffset + 2
    var depth = 1
    while (offset < length && depth > 0) {
        when {
            startsWith("/*", offset) -> {
                depth += 1
                offset += 2
            }
            startsWith("*/", offset) -> {
                depth -= 1
                offset += 2
            }
            else -> offset += 1
        }
    }
    return offset
}

private fun String.rawStringEndOffset(startOffset: Int): Int {
    val closingOffset = indexOf("\"\"\"", startOffset + 3)
    return if (closingOffset >= 0) closingOffset + 3 else length
}

private fun String.quotedLiteralEndOffset(startOffset: Int, quote: Char): Int {
    var offset = startOffset + 1
    while (offset < length) {
        when {
            this[offset] == '\\' -> offset = (offset + 2).coerceAtMost(length)
            this[offset] == quote -> return offset + 1
            else -> offset += 1
        }
    }
    return offset
}

private fun String.kotlinTokenType(): KotlinTokenType {
    return when (this) {
        "package" -> KotlinTokenType.PACKAGE_KEYWORD
        "class" -> KotlinTokenType.CLASS_KEYWORD
        "interface" -> KotlinTokenType.INTERFACE_KEYWORD
        "object" -> KotlinTokenType.OBJECT_KEYWORD
        "fun" -> KotlinTokenType.FUN_KEYWORD
        "typealias" -> KotlinTokenType.TYPE_ALIAS_KEYWORD
        "where" -> KotlinTokenType.WHERE_KEYWORD
        else -> KotlinTokenType.IDENTIFIER
    }
}

private fun List<KotlinToken>.packageName(content: String): String {
    val packageToken = firstOrNull { it.type == KotlinTokenType.PACKAGE_KEYWORD } ?: return ""
    return content.lineTail(packageToken.endOffset)
        .substringBefore("//")
        .trim()
        .removeSuffix(";")
        .trim()
}

private fun String.imports(): List<KotlinImport> {
    return lines()
        .mapIndexedNotNull { index, line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("import ")) return@mapIndexedNotNull null

            val importedFqName = trimmed
                .removePrefix("import ")
                .substringBefore("//")
                .substringBefore(" as ")
                .trim()
                .removeSuffix(";")
                .removeSuffix(".*")
                .trim()
                .takeIf { it.isNotEmpty() }

            KotlinImport(
                importedFqName = importedFqName,
                lineNumber = index + 1,
            )
        }
}

private fun List<KotlinToken>.topLevelDeclarations(packageName: String): List<KotlinDeclaration> {
    val declarations = mutableListOf<KotlinDeclaration>()
    var braceDepth = 0
    val modifiers = mutableSetOf<String>()

    forEachIndexed { index, token ->
        if (braceDepth == 0) {
            if (token.text in TOP_LEVEL_MODIFIER_TEXTS) {
                modifiers += token.text
            }

            val kind = token.type.declarationKind()
            if (kind != null) {
                if (!isClassLiteralToken(index)) {
                    declarationAt(index, kind, packageName, modifiers)?.let { declarations += it }
                }
                modifiers.clear()
            }
        }

        when (token.type) {
            KotlinTokenType.LBRACE -> braceDepth += 1
            KotlinTokenType.RBRACE -> braceDepth = (braceDepth - 1).coerceAtLeast(0)
            else -> Unit
        }
    }

    return declarations
}

private fun List<KotlinToken>.isClassLiteralToken(index: Int): Boolean {
    if (this[index].type != KotlinTokenType.CLASS_KEYWORD) return false
    return take(index)
        .lastOrNull { it.type != KotlinTokenType.WHITE_SPACE && it.type != KotlinTokenType.EOL_OR_SEMICOLON }
        ?.type == KotlinTokenType.COLONCOLON
}

private fun List<KotlinToken>.declarationAt(
    keywordIndex: Int,
    kind: KotlinDeclarationKind,
    packageName: String,
    modifiers: Set<String>,
): KotlinDeclaration? {
    val keyword = this[keywordIndex]
    val nameToken = drop(keywordIndex + 1)
        .firstOrNull { it.type == KotlinTokenType.IDENTIFIER }
        ?: return null
    val name = nameToken.text

    return KotlinDeclaration(
        name = name,
        qualifiedName = "$packageName.$name",
        kind = kind,
        isPublic = modifiers.none { it == "private" || it == "internal" || it == "protected" },
        superTypeNames = if (kind == KotlinDeclarationKind.CLASS ||
            kind == KotlinDeclarationKind.INTERFACE ||
            kind == KotlinDeclarationKind.OBJECT
        ) {
            superTypeNamesAfter(keywordIndex)
        } else {
            emptySet()
        },
        returnTypeName = if (kind == KotlinDeclarationKind.FUNCTION) {
            returnTypeNameAfter(nameToken)
        } else {
            null
        },
        lineNumber = keyword.lineNumber,
    )
}

private fun List<KotlinToken>.superTypeNamesAfter(keywordIndex: Int): Set<String> {
    var parenDepth = 0
    var inSuperTypes = false
    val superTypeNames = mutableSetOf<String>()

    drop(keywordIndex + 1).forEach { token ->
        when (token.type) {
            KotlinTokenType.LPAR -> parenDepth += 1
            KotlinTokenType.RPAR -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
            KotlinTokenType.COLON -> if (parenDepth == 0) inSuperTypes = true
            KotlinTokenType.LBRACE, KotlinTokenType.EQ, KotlinTokenType.WHERE_KEYWORD -> return superTypeNames
            KotlinTokenType.IDENTIFIER -> if (inSuperTypes) superTypeNames += token.text.toSimpleTypeName()
            else -> Unit
        }
    }

    return superTypeNames
}

private fun List<KotlinToken>.returnTypeNameAfter(nameToken: KotlinToken): String? {
    var parenDepth = 0
    var collectReturnType = false
    val returnType = StringBuilder()

    dropWhile { it !== nameToken }
        .drop(1)
        .forEach { token ->
            when (token.type) {
                KotlinTokenType.LPAR -> parenDepth += 1
                KotlinTokenType.RPAR -> parenDepth = (parenDepth - 1).coerceAtLeast(0)
                KotlinTokenType.COLON -> if (parenDepth == 0) collectReturnType = true
                KotlinTokenType.LBRACE, KotlinTokenType.EQ, KotlinTokenType.EOL_OR_SEMICOLON -> {
                    if (collectReturnType) return returnType.toString().toSimpleTypeName()
                }
                else -> if (collectReturnType) returnType.append(token.text)
            }
        }

    return returnType.toString()
        .takeIf { it.isNotBlank() }
        ?.toSimpleTypeName()
}

private fun KotlinTokenType.declarationKind(): KotlinDeclarationKind? {
    return when (this) {
        KotlinTokenType.CLASS_KEYWORD -> KotlinDeclarationKind.CLASS
        KotlinTokenType.INTERFACE_KEYWORD -> KotlinDeclarationKind.INTERFACE
        KotlinTokenType.OBJECT_KEYWORD -> KotlinDeclarationKind.OBJECT
        KotlinTokenType.FUN_KEYWORD -> KotlinDeclarationKind.FUNCTION
        KotlinTokenType.TYPE_ALIAS_KEYWORD -> KotlinDeclarationKind.TYPE_ALIAS
        else -> null
    }
}

private fun String.lineTail(startOffset: Int): String {
    val endOffset = indexOf('\n', startOffset).takeIf { it >= 0 } ?: length
    return substring(startOffset, endOffset)
}

private fun String.toSimpleTypeName(): String {
    return substringBefore("<")
        .substringBefore("(")
        .substringAfterLast(".")
        .trim()
}

private fun String.toGradleAccessor(): String {
    return split(Regex("[^A-Za-z0-9]+"))
        .filter { it.isNotEmpty() }
        .mapIndexed { index, part ->
            if (index == 0) {
                part.replaceFirstChar { it.lowercase() }
            } else {
                part.replaceFirstChar { it.uppercase() }
            }
        }
        .joinToString("")
}

private fun String.lineStarts(): List<Int> {
    val starts = mutableListOf(0)
    forEachIndexed { index, char ->
        if (char == '\n' && index + 1 < length) {
            starts += index + 1
        }
    }
    return starts
}

private val TOP_LEVEL_MODIFIER_TEXTS = setOf(
    "public",
    "private",
    "internal",
    "protected",
    "data",
    "sealed",
    "abstract",
    "open",
    "value",
)

private fun List<Int>.lineNumberFor(offset: Int): Int {
    val index = binarySearch(offset)
    return if (index >= 0) {
        index + 1
    } else {
        -index - 1
    }
}
