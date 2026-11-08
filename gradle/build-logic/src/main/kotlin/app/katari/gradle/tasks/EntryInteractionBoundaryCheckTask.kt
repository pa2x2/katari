package app.katari.gradle.tasks

import app.katari.gradle.kotlinsource.KotlinDeclaration
import app.katari.gradle.kotlinsource.KotlinDeclarationKind
import app.katari.gradle.kotlinsource.KotlinImport
import app.katari.gradle.kotlinsource.KotlinReference
import app.katari.gradle.kotlinsource.KotlinSourceFile
import app.katari.gradle.kotlinsource.KotlinSourceIndex
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
        val findings = EntryInteractionBoundaryRules(root, sourceIndex, typeModules).check().toMutableList()
        findings += checkEntryViewerSettingsProjectionBoundaries(
            sourceIndex.files.map { source ->
                EntryViewerSettingsProjectionBoundarySource(source.relativePath, source.content)
            },
        ).map { finding ->
            Finding(
                relativePath = finding.relativePath,
                lineNumber = finding.lineNumber,
                reason = finding.reason,
            )
        }

        if (findings.isNotEmpty()) {
            val maxFindings = 80
            val rendered = findings
                .take(maxFindings)
                .joinToString(separator = "\n") { finding ->
                    val path = sourceIndex.filePath(finding.relativePath)
                    val location = if (finding.lineNumber != null) "$path:${finding.lineNumber}" else path
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
        return SOURCE_MODULES
            .flatMap { module -> PRODUCTION_SOURCE_DIRECTORIES.map { root.resolve("$module/src/$it") } }
            .plus(typeModules.flatMap { it.sourceRoots })
            .filter { it.isDirectory }
            .flatMap { directory ->
                directory.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .filterNot { it.invariantSeparatorsPath.contains("/build/") }
                    .toList()
            }
    }

    companion object {
        private val SOURCE_MODULES = listOf(
            "app",
            "app-ui",
            "core/data",
            "core/domain",
            "core/presentation-core",
            "features/presentation-widget",
            "sources/source-local",
            "sources/source-compat",
            "sources/source-api",
            "sources/entry-source-api",
            "features/entry-interactions",
            "features/entry-interactions/api",
            "features/entry-interactions/spi",
            "features/entry-interactions/host",
            "features/backup",
            "sources/extension-runtime",
            "desktop",
        )
    }
}

private class EntryInteractionBoundaryRules(
    private val root: File,
    private val sourceIndex: KotlinSourceIndex,
    private val typeModules: List<TypeModule>,
) {
    private val processorInterfaceNames = sourceIndex.files
        .asSequence()
        .filter { it.relativePath.startsWith("features/entry-interactions/spi/src/main/") }
        .flatMap { it.topLevelDeclarations.asSequence() }
        .filter { it.kind == KotlinDeclarationKind.INTERFACE }
        .map { it.name }
        .filter { it.startsWith("Entry") && it.endsWith("Processor") }
        .toSet()

    private val internalApiNames = buildSet {
        val spiFiles = sourceIndex.files.filter {
            it.relativePath.startsWith("features/entry-interactions/spi/src/main/")
        }
        addAll(
            spiFiles
                .flatMap(KotlinSourceFile::topLevelDeclarations)
                .filter { it.isPublic && it.kind != KotlinDeclarationKind.FUNCTION }
                .map(KotlinDeclaration::name),
        )
        addAll(spiFiles.asSequence().flatMap(KotlinSourceFile::publicTopLevelPropertyNames))
    }
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
        checkRootGradleDependencies(findings)
        checkTypeModuleGradleDependencies(findings)

        sourceIndex.files.forEach { file ->
            checkForbiddenImports(file, findings)
            checkRootCompositionTypeModuleImports(file, findings)
            checkAppLocalInteractionDeclaration(file, findings)
            checkLegacyInteractionApis(file, findings)
            checkInternalApiReferences(file, findings)
            checkLibraryProgressDomainPortReferences(file, findings)
            checkCatalogueFeatureBypass(file, findings)
            checkLegacySourceCompatibilityBoundary(file, findings)
            checkSourceActionFeatureBypass(file, findings)
            checkDownloadActionPolicyBypass(file, findings)
            checkSourceRefreshFeatureBypass(file, findings)
            checkSourceRefreshMechanicsBypass(file, findings)
            checkMeteredSourcePolicyBypass(file, findings)
            checkChildWebViewFeatureBypass(file, findings)
            checkProcessorImplementationReferences(file, findings)
            checkRuntimeEntryPointReferences(file, findings)
            checkRuntimeInternalReferences(file, findings)
            checkDownloadRuntimeReferences(file, findings)
            checkSourceMediaResolutionReferences(file, findings)
            checkMediaCacheMaintenanceReferences(file, findings)
            checkMediaSessionConsequenceBypass(file, findings)
            checkBookmarkMutationBypass(file, findings)
            checkProfileDeletionBypass(file, findings)
            checkProfilePreferenceOwnerBypass(file, findings)
            checkExhaustiveEntryTypeMappings(file, findings)
            checkSuspiciousTypeBranches(file, findings)
        }

        checkTypeModulePublicApis(findings)
        checkApplicationApiDispatchContracts(findings)
        findings += checkEntryMergeBoundaries(sourceIndex.files.map(KotlinSourceFile::toEntryMergeBoundarySource))
            .map { finding ->
                Finding(
                    relativePath = finding.relativePath,
                    lineNumber = finding.lineNumber,
                    reason = finding.reason,
                )
            }
        findings +=
            checkEntryMigrationBoundaries(sourceIndex.files.map(KotlinSourceFile::toEntryMigrationBoundarySource))
                .map { finding ->
                    Finding(
                        relativePath = finding.relativePath,
                        lineNumber = finding.lineNumber,
                        reason = finding.reason,
                    )
                }

        return findings
    }

    private fun checkAppLocalInteractionDeclaration(
        file: KotlinSourceFile,
        findings: MutableList<Finding>,
    ) {
        if (file.isTestPath() || !file.isApplicationLayer()) return

        file.topLevelDeclarations
            .filter { declaration ->
                declaration.name.startsWith("Entry") && declaration.name.endsWith("Interaction")
            }
            .forEach { declaration ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = declaration.lineNumber,
                    reason = "application Entry behavior must expose a Feature boundary; app-local Interaction " +
                        "declarations recreate a parallel application API: ${declaration.name}",
                )
            }
    }

    private fun checkApplicationApiDispatchContracts(findings: MutableList<Finding>) {
        sourceIndex.files
            .filter { it.relativePath.startsWith("features/entry-interactions/api/src/main/") }
            .forEach { file ->
                file.topLevelDeclarations
                    .filter { declaration ->
                        declaration.isPublic &&
                            declaration.kind == KotlinDeclarationKind.INTERFACE &&
                            (declaration.name == "EntryInteractions" || declaration.name.endsWith("Interaction"))
                    }
                    .forEach { declaration ->
                        findings += Finding(
                            relativePath = file.relativePath,
                            lineNumber = declaration.lineNumber,
                            reason = "raw Entry interaction dispatch must live in provider SPI, not " +
                                "application-facing API: ${declaration.name}",
                        )
                    }
            }
    }

    private fun checkProfilePreferenceOwnerBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val lines = file.content.lines()
        lines.forEachIndexed { index, line ->
            if (!ACTIVE_PROFILE_STORE_ACCESS.containsMatchIn(line)) return@forEachIndexed

            val currentCodeLine = line.substringBefore("//").trim()
            val previousCodeLine = lines.take(index)
                .map { it.substringBefore("//").trim() }
                .lastOrNull { it.isNotBlank() }
            val bindsOwnerInstaller = currentCodeLine.contains("ProfilePreferenceOwnerInstaller(") ||
                (
                    previousCodeLine?.contains("ProfilePreferenceOwnerInstaller(") == true &&
                        previousCodeLine.endsWith("{")
                    )
            if (bindsOwnerInstaller) return@forEachIndexed

            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = index + 1,
                reason = "active profile/private preference stores may only bind ProfilePreferenceOwnerInstaller; " +
                    "runtime preference owners must be created from an installed handle",
            )
        }
    }

    private fun checkMediaSessionConsequenceBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath() || file.owningTypeModule() == null) return
        if (!file.content.contains("MediaSessionProcessor")) return
        if (file.relativePath.endsWith("MediaSessionProcessor.kt")) return

        MEDIA_SESSION_DIRECT_CONSEQUENCE_OPERATIONS.forEach { operation ->
            file.findReference(operation)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason =
                    "media runtimes must emit EntryMediaSessionEvent facts; $operation belongs to the shared " +
                        "media-session consequences",
                )
            }
        }
    }

    private fun checkBookmarkMutationBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath() || file.owningTypeModule() == null) return
        val ownsBookmarkProvider = file.topLevelDeclarations.any { declaration ->
            "EntryBookmarkProcessor" in declaration.superTypeNames
        }
        if (ownsBookmarkProvider) return
        if (!DIRECT_BOOKMARK_PERSISTENCE.containsMatchIn(file.content)) return

        findings += Finding(
            relativePath = file.relativePath,
            lineNumber = file.lineNumberOf("bookmark"),
            reason = "type runtimes must mutate bookmarks through EntryBookmarkFeature; direct bookmark persistence " +
                "bypasses contributed behavior",
        )
    }

    private fun checkProfileDeletionBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath() || PROFILE_FEATURE_ROOTS.none(file.relativePath::startsWith)) return

        file.findReference("deleteByProfile")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "profile deletion must use relational ownership and EntryDestructiveRemovalFeature; " +
                    "a hand-maintained deleteByProfile list is a parallel cleanup authority",
            )
        }

        if (!file.relativePath.endsWith("/core/ProfileManager.kt")) return
        if (!file.content.contains("permanentlyDeleteProfile")) return
        val usesDestructiveRemoval = file.findReference("EntryDestructiveRemovalFeature") != null &&
            DESTRUCTIVE_REMOVAL_INVOCATION.containsMatchIn(file.content)
        if (!usesDestructiveRemoval) {
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = file.lineNumberOf("permanentlyDeleteProfile"),
                reason = "permanent profile deletion must route its Entries through EntryDestructiveRemovalFeature",
            )
        }
    }

    private fun checkAppGradleDependencies(findings: MutableList<Finding>) {
        val forbiddenDependencies = typeModules
            .map { "projects.features.entryInteractions.${it.gradleAccessor}" }
            .plus("projects.features.entryInteractions.api")
            .plus("projects.features.entryInteractions.spi")

        applicationBuildFiles().forEach { file ->
            val relativePath = file.relativeTo(root).invariantSeparatorsPath
            val module = relativePath.removeSuffix("/build.gradle.kts")
            file.readLines().forEachIndexed { index, line ->
                forbiddenDependencies.forEach { dependency ->
                    if (line.contains(dependency)) {
                        findings += Finding(
                            relativePath = relativePath,
                            lineNumber = index + 1,
                            reason = "$module must consume Entry features through " +
                                "projects.features.entryInteractions, not: $dependency",
                        )
                    }
                }
            }
        }
    }

    /** The build files of the modules that apply an application plugin, which each compose an app graph. */
    private fun applicationBuildFiles(): List<File> = root.walkTopDown()
        .onEnter { directory ->
            directory == root || (directory.name !in NON_MODULE_DIRECTORIES && !directory.isHidden)
        }
        .filter { it.isFile && it.name == "build.gradle.kts" }
        .filter { APPLICATION_PLUGIN.containsMatchIn(it.readText()) }
        .sortedBy { it.invariantSeparatorsPath }
        .toList()

    private fun checkTypeModuleGradleDependencies(findings: MutableList<Finding>) {
        typeModules.forEach { module ->
            val file = module.gradleFile
            if (!file.isFile) return@forEach

            file.readLines().forEachIndexed { index, line ->
                if (line.contains("testImplementation")) return@forEachIndexed
                if (
                    line.contains("projects.features.entryInteractions)") ||
                    line.contains("projects.features.entryInteractions,")
                ) {
                    findings += Finding(
                        relativePath = module.gradleRelativePath,
                        lineNumber = index + 1,
                        reason = "type interaction modules must depend on " +
                            "projects.features.entryInteractions.spi, not root",
                    )
                }
            }
        }
    }

    private fun checkRootGradleDependencies(findings: MutableList<Finding>) {
        val file = root.resolve("features/entry-interactions/build.gradle.kts")
        if (!file.isFile) return

        file.readLines().forEachIndexed { index, line ->
            if (line.contains("api(projects.features.entryInteractions.spi)")) {
                findings += Finding(
                    relativePath = "features/entry-interactions/build.gradle.kts",
                    lineNumber = index + 1,
                    reason = "root Entry interactions must not export the provider SPI to application consumers",
                )
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
        if (!file.relativePath.startsWith("features/entry-interactions/src/main/")) return

        file.imports.forEach { import ->
            val importedFqName = import.importedFqName ?: return@forEach
            val module = typeModules.firstOrNull { importedFqName.startsWith("${it.packagePrefix}.") }
                ?: return@forEach

            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = import.lineNumber,
                reason = "root Entry interactions must not import ${module.name} type-module symbols; " +
                    "type modules contribute to the dependency graph: $importedFqName",
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

    private fun checkLibraryProgressDomainPortReferences(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val allowedPaths = setOf(
            "core/domain/src/main/java/tachiyomi/domain/entry/service/EntryLibraryProgressResolution.kt",
            "core/domain/src/main/java/tachiyomi/domain/entry/interactor/GetLibraryEntries.kt",
            "features/entry-interactions/api/src/main/java/app/katari/entry/interactions/library/" +
                "EntryLibraryProgressFeature.kt",
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/library/EntryLibraryBindings.kt",
        )
        if (file.relativePath in allowedPaths) return

        file.findReference("EntryLibraryProgressResolutionPort")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "Library Progress domain port is reserved for Domain assembly and Feature composition; " +
                    "application consumers must use EntryLibraryProgressFeature",
            )
        }
    }

    private fun checkCatalogueFeatureBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val isApplicationLayer = file.relativePath.startsWith("app/src/main/") ||
            file.relativePath.startsWith("core/data/src/main/") ||
            file.relativePath.startsWith("core/domain/src/main/") ||
            file.relativePath.startsWith("core/presentation-core/src/main/") ||
            file.relativePath.startsWith("features/presentation-widget/src/main/")

        if (isApplicationLayer) {
            if (file.relativePath !in SOURCE_DESCRIPTION_PORT_FILES) {
                file.findReference("EntrySourceDescriptionResolutionPort")?.let { reference ->
                    findings += Finding(
                        relativePath = file.relativePath,
                        lineNumber = reference.lineNumber,
                        reason = "application consumers must use EntryCatalogueFeature, not its Domain assembly port",
                    )
                }
            }

            file.imports
                .filter { it.importedFqName in RAW_SOURCE_DESCRIPTION_IMPORTS }
                .forEach { import ->
                    findings += Finding(
                        relativePath = file.relativePath,
                        lineNumber = import.lineNumber,
                        reason =
                        "application source availability and description must use EntryCatalogueFeature, " +
                            "not raw source contract ${import.importedFqName}",
                    )
                }
        }

        val isCatalogueConsumerLayer = isApplicationLayer ||
            (file.relativePath.startsWith("features/entry-interactions/") && "/src/main/" in file.relativePath)
        if (!isCatalogueConsumerLayer) return

        if (file.relativePath !in CATALOGUE_PROVIDER_ASSEMBLY_FILES) {
            RAW_CATALOGUE_OPERATION_NAMES.forEach { operation ->
                file.findReference(operation)?.let { reference ->
                    findings += Finding(
                        relativePath = file.relativePath,
                        lineNumber = reference.lineNumber,
                        reason = "catalogue provider execution must use EntryCatalogueFeature, " +
                            "not raw $operation dispatch",
                    )
                }
            }
        }
    }

    private fun checkLegacySourceCompatibilityBoundary(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath() || file.relativePath.startsWith("sources/source-compat/src/main/")) return

        file.imports
            .filter { it.importedFqName == LEGACY_MANGA_SOURCE_ADAPTER }
            .forEach { import ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = import.lineNumber,
                    reason = "legacy Manga adapter identity is confined to source-compat; consumers must use " +
                        "current source contracts or a compatibility operation",
                )
            }

        file.imports
            .filter { it.importedFqName == LEGACY_UNMETERED_SOURCE }
            .forEach { import ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = import.lineNumber,
                    reason = "legacy UnmeteredSource is source-compat input, not a current runtime policy contract",
                )
            }
    }

    private fun checkSourceActionFeatureBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val isApplicationLayer = file.relativePath.startsWith("app/src/main/") ||
            file.relativePath.startsWith("core/data/src/main/") ||
            file.relativePath.startsWith("core/domain/src/main/") ||
            file.relativePath.startsWith("core/presentation-core/src/main/") ||
            file.relativePath.startsWith("features/presentation-widget/src/main/")
        if (!isApplicationLayer) return

        RAW_SOURCE_ACTION_IMPORTS.forEach { (simpleName, qualifiedName) ->
            file.findReference(simpleName)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "application source actions must use their Entry Feature boundary, not raw source " +
                        "contract $qualifiedName",
                )
            }
        }
    }

    private fun checkDownloadActionPolicyBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val isApplicationLayer = file.relativePath.startsWith("app/src/main/") ||
            file.relativePath.startsWith("core/data/src/main/") ||
            file.relativePath.startsWith("core/domain/src/main/") ||
            file.relativePath.startsWith("core/presentation-core/src/main/") ||
            file.relativePath.startsWith("features/presentation-widget/src/main/")
        if (!isApplicationLayer) return

        listOf("EntryDownloadActionTarget", "EntryDownloadSourceAccess").forEach { policyType ->
            file.findReference(policyType)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "Download applicability evidence is owned by EntryDownloadActionFeature, " +
                        "not application policy type $policyType",
                )
            }
        }

        if (file.findReference("EntryDownloadActionFeature") == null) return
        listOf("MangaReaderSettings", "skipFiltered", "isLocalOrStub").forEach { policyInput ->
            file.findReference(policyInput)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "Download consumers must pass factual requests to EntryDownloadActionFeature; " +
                        "$policyInput must not select generic Download behavior",
                )
            }
        }
    }

    private fun checkSourceRefreshFeatureBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val allowedPaths = setOf(
            "core/domain/src/main/java/tachiyomi/domain/entry/interactor/SyncEntryWithSource.kt",
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/source/" +
                "DefaultEntrySourceRefreshFeature.kt",
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/source/EntrySourceRefreshBindings.kt",
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/source/SourceRefreshBindings.kt",
        )
        if (file.relativePath in allowedPaths || isAppGraphCompositionPath(file.relativePath)) return

        file.findReference("SyncEntryWithSource")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "Entry refresh consumers must use EntrySourceRefreshFeature, " +
                    "not raw SyncEntryWithSource mechanics",
            )
        }
    }

    private fun checkSourceRefreshMechanicsBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val ownsSourceContract = file.relativePath.startsWith("sources/entry-source-api/src/") ||
            file.relativePath.startsWith("sources/source-api/src/") ||
            file.relativePath.startsWith("sources/source-compat/src/") ||
            file.relativePath.startsWith("sources/source-local/src/") ||
            file.relativePath in SOURCE_REFRESH_MECHANICS_OWNERS
        if (ownsSourceContract) return

        SOURCE_REFRESH_MECHANICS_CONTRACTS.forEach { contract ->
            file.findReference(contract)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "$contract interpretation belongs to SyncEntryWithSource source-refresh mechanics",
                )
            }
        }
    }

    private fun checkMeteredSourcePolicyBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val isGenericConsumer = file.relativePath.startsWith("app/src/main/") ||
            file.relativePath.startsWith("core/data/src/main/") ||
            file.relativePath.startsWith("core/domain/src/main/") ||
            file.relativePath.startsWith("core/presentation-core/src/main/") ||
            file.relativePath.startsWith("features/presentation-widget/src/main/")
        if (!isGenericConsumer) return

        file.findReference("UnmeteredSource")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "application Library queue warning policy must use " +
                    "EntryLibraryUpdateNotificationFeature, not raw UnmeteredSource context",
            )
        }
    }

    private fun checkChildWebViewFeatureBypass(file: KotlinSourceFile, findings: MutableList<Finding>) {
        if (file.isTestPath()) return
        val ownsContract = file.relativePath.startsWith("sources/source-compat/src/main/") ||
            file.relativePath.startsWith("sources/source-local/src/main/") ||
            file.relativePath.startsWith("sources/entry-source-api/src/") ||
            file.relativePath ==
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/source/DefaultEntryWebViewFeature.kt"
        if (ownsContract) return

        file.findReference("ChapterWebViewSource")?.let { reference ->
            findings += Finding(
                relativePath = file.relativePath,
                lineNumber = reference.lineNumber,
                reason = "canonical child WebView actions must use EntryWebViewFeature, not raw ChapterWebViewSource",
            )
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
        if (file.isTestPath()) return

        downloadRuntimeSymbols.forEach { symbol ->
            if (file.isOwnedBy(symbol.owner)) return@forEach

            file.findReference(symbol)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "direct ${symbol.simpleName} download runtime reference outside " +
                        "${symbol.owner.name} type module",
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

        MEDIA_CACHE_MAINTENANCE_FORBIDDEN_TYPES.forEach { typeName ->
            file.findReference(typeName)?.let { reference ->
                findings += Finding(
                    relativePath = file.relativePath,
                    lineNumber = reference.lineNumber,
                    reason = "settings/UI cache maintenance must use EntryMediaCacheFeature, not a raw cache " +
                        "implementation or host port: $typeName",
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
            reason = "generic EntryType MANGA/ANIME mapping must use EntryTypePresentationFeature or an approved " +
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
                        if (declaration.isBindingContainer(file)) return@forEach
                        if (declaration.isGraphEntryPoint(file)) return@forEach

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

    // Type modules reach the app only through the binding containers the Metro graph aggregates, and Metro
    // aggregates only public containers.
    private fun KotlinDeclaration.isBindingContainer(file: KotlinSourceFile): Boolean {
        return kind == KotlinDeclarationKind.OBJECT &&
            name.endsWith("Bindings") &&
            file.relativePath.endsWith("/$name.kt")
    }

    // Android-constructed components of a type module reach the graph through a contributed `<Owner>Graph`
    // interface. Its members must be public, so internal services travel in an `<Owner>Dependencies` holder whose
    // constructor and members stay internal.
    private fun KotlinDeclaration.isGraphEntryPoint(file: KotlinSourceFile): Boolean {
        val owner = file.relativePath.substringAfterLast('/').removeSuffix("Graph.kt")
        if (!file.relativePath.endsWith("/${owner}Graph.kt")) return false
        return (kind == KotlinDeclarationKind.INTERFACE && name == "${owner}Graph") ||
            (kind == KotlinDeclarationKind.CLASS && name == "${owner}Dependencies")
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
        return relativePath.startsWith("features/entry-interactions/src/main/") ||
            relativePath.startsWith("features/entry-interactions/api/src/main/") ||
            relativePath.startsWith("features/entry-interactions/spi/src/main/") ||
            owningTypeModule() != null ||
            isTestPath()
    }

    private fun KotlinSourceFile.isApplicationLayer(): Boolean {
        return APPLICATION_LAYER_ROOTS.any { root -> relativePath.startsWith(root) }
    }

    private fun KotlinSourceFile.isSourceMediaResolutionGuardedPath(): Boolean {
        if (isRootOrTypeModuleOrTestPath()) return false
        if (!SOURCE_MEDIA_RESOLUTION_GUARDED_ROOTS.any { relativePath.startsWith(it) }) return false
        return relativePath !in SOURCE_MEDIA_RESOLUTION_ALLOWED_FILES
    }

    private fun KotlinSourceFile.isMediaCacheMaintenanceGuardedPath(): Boolean {
        if (isRootOrTypeModuleOrTestPath()) return false
        return MEDIA_CACHE_MAINTENANCE_GUARDED_ROOTS.any { relativePath.startsWith(it) }
    }

    private fun KotlinSourceFile.isExhaustiveEntryTypeMappingAllowedPath(): Boolean {
        return isTestPath() || relativePath in EXHAUSTIVE_ENTRY_TYPE_MAPPING_ALLOWED_FILES
    }

    private fun KotlinSourceFile.isStrictImportCheckedPath(): Boolean {
        if (isTestPath()) return false
        return STRICT_IMPORT_CHECKED_ROOTS.any { relativePath.startsWith(it) }
    }

    private fun KotlinSourceFile.isTypeBranchAllowedPath(): Boolean {
        return isRootOrTypeModuleOrTestPath()
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
            "core/data/src/main/",
            "core/domain/src/main/",
            "core/presentation-core/src/main/",
            "features/presentation-widget/src/main/",
            "sources/source-local/src/main/",
            "sources/source-compat/src/main/",
        )

        private val APPLICATION_LAYER_ROOTS = listOf(
            "app/src/main/",
            "core/data/src/main/",
            "core/domain/src/main/",
            "core/presentation-core/src/main/",
            "features/presentation-widget/src/main/",
        )

        private val LEGACY_INTERACTION_APIS = listOf(
            "openEntryHandlerFor",
            "continueEntryHandlerFor",
            "downloadEntryHandlerFor",
            "EntryDownloader",
        )

        private const val LEGACY_MANGA_PAGE_FQ_NAME = "eu.kanade.tachiyomi.source.model.Page"
        private const val LEGACY_MANGA_SOURCE_ADAPTER =
            "eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter"
        private const val LEGACY_UNMETERED_SOURCE = "eu.kanade.tachiyomi.source.UnmeteredSource"

        private val SOURCE_DESCRIPTION_PORT_FILES = setOf(
            "core/data/src/main/java/tachiyomi/data/source/SourceRepositoryImpl.kt",
            "core/domain/src/main/java/tachiyomi/domain/entry/interactor/GetLibraryEntries.kt",
            "core/domain/src/main/java/tachiyomi/domain/source/service/EntrySourceDescriptionResolutionPort.kt",
        )

        private val RAW_SOURCE_DESCRIPTION_IMPORTS = setOf(
            "eu.kanade.tachiyomi.source.entry.EntryCatalogueSource",
            "eu.kanade.tachiyomi.source.entry.EntryItemOrientationProvider",
            "eu.kanade.tachiyomi.source.entry.SourceMetadata",
            "eu.kanade.tachiyomi.source.entry.entryItemOrientation",
            "eu.kanade.tachiyomi.source.entry.supportedEntryTypes",
        )

        private val CATALOGUE_PROVIDER_ASSEMBLY_FILES = setOf(
            "core/domain/src/main/java/tachiyomi/domain/source/model/StubSource.kt",
            "core/domain/src/main/java/tachiyomi/domain/source/model/UnifiedStubSource.kt",
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/catalogue/host/" +
                "SourceManagerEntryCatalogueProviderHost.kt",
        )

        private val RAW_CATALOGUE_OPERATION_NAMES = setOf(
            "getPopularContent",
            "getLatestUpdates",
            "getSearchContent",
            "getFilterList",
            "getCatalogueSource",
            "getCatalogueSources",
            "toCatalogSource",
        )

        private val RAW_SOURCE_ACTION_IMPORTS = mapOf(
            "ConfigurableSource" to "eu.kanade.tachiyomi.source.entry.ConfigurableSource",
            "SourceHomePage" to "eu.kanade.tachiyomi.source.entry.SourceHomePage",
            "WebViewSource" to "eu.kanade.tachiyomi.source.entry.WebViewSource",
            "ResolvableSource" to "eu.kanade.tachiyomi.source.entry.ResolvableSource",
            "EntryPreviewSource" to "eu.kanade.tachiyomi.source.entry.EntryPreviewSource",
            "RelatedEntriesSource" to "eu.kanade.tachiyomi.source.entry.RelatedEntriesSource",
            "EntryImageSource" to "eu.kanade.tachiyomi.source.entry.EntryImageSource",
            "ArtworkSource" to "eu.kanade.tachiyomi.source.entry.ArtworkSource",
            "DeferredStreamSource" to "eu.kanade.tachiyomi.source.entry.DeferredStreamSource",
        )

        // The whole-list refresh and the older parts of a list arriving in parts save chapters through one saver
        private val SOURCE_REFRESH_MECHANICS_OWNERS = setOf(
            "core/domain/src/main/java/tachiyomi/domain/entry/interactor/SyncEntryWithSource.kt",
            "core/domain/src/main/java/tachiyomi/domain/entry/interactor/SourceChapterSaver.kt",
        )

        private val SOURCE_REFRESH_MECHANICS_CONTRACTS = listOf(
            "EmptyChapterListSource",
            "ChapterNumberRecognitionSource",
        )

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
            "core/data/src/main/java/",
            "core/domain/src/main/java/",
            "core/presentation-core/src/main/java/",
            "features/presentation-widget/src/main/java/",
        )

        private val SOURCE_MEDIA_RESOLUTION_ALLOWED_FILES = setOf(
            "core/domain/src/main/java/tachiyomi/domain/source/model/StubSource.kt",
        )

        private val EXHAUSTIVE_ENTRY_TYPE_MAPPING_ALLOWED_FILES = setOf(
            "features/entry-interactions/src/main/java/app/katari/entry/interactions/library/" +
                "EntryLibraryUpdateNotificationRouting.kt",
        )

        private val MEDIA_CACHE_MAINTENANCE_FORBIDDEN_TYPES = setOf(
            "EntryPageImageCache",
            "EntryPlayerCache",
            "MangaPageCache",
            "AppMangaPageImageCache",
            "ReaderPageCache",
            "ExoPlayerMediaCache",
        )

        private val MEDIA_CACHE_MAINTENANCE_GUARDED_ROOTS = listOf(
            "app/src/main/java/eu/kanade/presentation/",
            "app/src/main/java/eu/kanade/tachiyomi/ui/",
            "core/presentation-core/src/main/java/",
            "features/presentation-widget/src/main/java/",
        )

        private val MEDIA_SESSION_DIRECT_CONSEQUENCE_OPERATIONS = setOf(
            "mergeAndSyncChild",
            "upsertHistory",
            "EntryDownloadLifecycleEventSink",
            "EntryDownloadLifecycleFeature",
            "EntryMediaSessionIncognitoState",
        )

        private val PROFILE_FEATURE_ROOTS = listOf(
            "app/src/main/java/app/katari/profiles/",
            "app-ui/src/commonMain/kotlin/app/katari/profiles/",
            "core/data/src/commonMain/kotlin/app/katari/profiles/",
        )

        private val DIRECT_BOOKMARK_PERSISTENCE = Regex(
            """(?s)\bupdateAll\s*\(.*?\.copy\s*\(\s*bookmark\s*=""",
        )

        private val DESTRUCTIVE_REMOVAL_INVOCATION = Regex(
            """\bdestructiveRemoval\s*\.\s*remove\s*\(""",
        )

        private val TYPE_BRANCH_PATTERNS = listOf(
            Regex("""\bEntryType\.[A-Z_]+\b"""),
            Regex("""\bentry\.type\b"""),
            Regex("""\bentryType\b"""),
        )

        private val APPLICATION_PLUGIN = Regex("""alias\(\s*katari\.plugins\.\w+\.application\s*\)""")

        private val NON_MODULE_DIRECTORIES = setOf("build", "gradle", "node_modules", "website")

        private val ACTIVE_PROFILE_STORE_ACCESS = Regex(
            """\.(?:profileStore|privateStore|appStateStore)\(\s*\)""",
        )

        private val PUBLIC_TYPE_MODULE_ANDROID_COMPONENT_FILES = setOf(
            "features/entry-interactions/anime/src/main/java/eu/kanade/tachiyomi/ui/video/player/" +
                "VideoPlayerActivity.kt",
            "features/entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/ReaderActivity.kt",
            "features/entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/" +
                "ReaderNavigationOverlayView.kt",
            "features/entry-interactions/manga/src/main/java/eu/kanade/tachiyomi/ui/reader/viewer/ReaderButton.kt",
        )
    }
}

private val PRODUCTION_SOURCE_DIRECTORIES = listOf(
    "main/java",
    "main/kotlin",
    "commonMain/kotlin",
    "androidMain/kotlin",
    "desktopMain/kotlin",
)

private fun KotlinSourceFile.toEntryMergeBoundarySource(): EntryMergeBoundarySource {
    val referenceLines = buildMap {
        imports.forEach { import ->
            val name = import.importedFqName?.substringAfterLast(".") ?: return@forEach
            putIfAbsent(name, import.lineNumber)
        }
        references.forEach { reference -> putIfAbsent(reference.name, reference.lineNumber) }
    }
    return EntryMergeBoundarySource(
        relativePath = relativePath,
        content = content,
        declarations = topLevelDeclarations.map { declaration ->
            EntryMergeBoundaryDeclaration(
                name = declaration.name,
                isPublic = declaration.isPublic,
                lineNumber = declaration.lineNumber,
            )
        },
        references = referenceLines,
    )
}

private fun KotlinSourceFile.toEntryMigrationBoundarySource(): EntryMigrationBoundarySource {
    val referenceLines = buildMap {
        imports.forEach { import ->
            val name = import.importedFqName?.substringAfterLast(".") ?: return@forEach
            putIfAbsent(name, import.lineNumber)
        }
        references.forEach { reference -> putIfAbsent(reference.name, reference.lineNumber) }
    }
    return EntryMigrationBoundarySource(
        relativePath = relativePath,
        content = content,
        declarations = topLevelDeclarations.map { declaration ->
            EntryMigrationBoundaryDeclaration(
                name = declaration.name,
                isPublic = declaration.isPublic,
                lineNumber = declaration.lineNumber,
            )
        },
        references = referenceLines,
    )
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
    val sourceRoots: List<File>,
    val gradleFile: File,
) {
    val packagePrefix: String = "app.katari.entry.interactions.$name"
    val gradleAccessor: String = name.toGradleAccessor()
    val gradleRelativePath: String = "$relativePath/build.gradle.kts"

    companion object {
        /**
         * Every module under `features/entry-interactions/` that isn't infrastructure is a type module. One with none
         * of [TYPE_MODULE_SOURCE_ROOTS] fails the check, so moving its sources where the check doesn't read them can't
         * leave it unchecked.
         */
        fun discover(root: File): List<TypeModule> {
            val entryInteractions = root.resolve("features/entry-interactions")
            return entryInteractions.listFiles()
                .orEmpty()
                .filter { it.isDirectory && it.name !in INFRASTRUCTURE_MODULES }
                .filter { it.resolve("build.gradle.kts").isFile }
                .map { directory ->
                    val relativePath = "features/entry-interactions/${directory.name}"
                    val sourceRoots = TYPE_MODULE_SOURCE_ROOTS.map(directory::resolve).filter(File::isDirectory)
                    if (sourceRoots.isEmpty()) {
                        throw GradleException(
                            "Entry interaction boundary check failed:\n- $relativePath: type module has none of the " +
                                "source roots the check reads: ${TYPE_MODULE_SOURCE_ROOTS.joinToString()}",
                        )
                    }
                    TypeModule(
                        name = directory.name,
                        relativePath = relativePath,
                        sourceRoots = sourceRoots,
                        gradleFile = directory.resolve("build.gradle.kts"),
                    )
                }
                .sortedBy { it.name }
        }
    }
}

private val TYPE_MODULE_SOURCE_ROOTS = listOf(
    "src/main/java",
    "src/commonMain/kotlin",
    "src/androidMain/kotlin",
    "src/desktopMain/kotlin",
)

private val INFRASTRUCTURE_MODULES = setOf("api", "spi", "download-notification", "host")

private fun KotlinImport.startsWith(packagePrefix: String): Boolean {
    return importedFqName == packagePrefix ||
        importedFqName?.startsWith("$packagePrefix.") == true
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
