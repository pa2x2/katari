package app.katari.gradle.platformparity

import java.io.File

/** The repository's Kotlin sources, as the platform checks read them, leaving out build outputs and the website. */
internal fun File.platformSources(): List<PlatformSource> = repositoryFiles { it.extension == "kt" }
    .map { PlatformSource(it.relativeTo(this).invariantSeparatorsPath, it.readText()) }

/** Every module's `build.gradle.kts`, by the module's directory. */
internal fun File.platformBuildFiles(): List<PlatformBuildFile> = repositoryFiles { it.name == "build.gradle.kts" }
    .filter { it.parentFile != this }
    .map { PlatformBuildFile(it.parentFile.relativeTo(this).invariantSeparatorsPath, it.readText()) }

private fun File.repositoryFiles(include: (File) -> Boolean): List<File> = walkTopDown()
    .onEnter { directory -> directory == this || (directory.name !in SKIPPED_DIRECTORIES && !directory.isHidden) }
    .filter { it.isFile && include(it) }
    .toList()

private val SKIPPED_DIRECTORIES = setOf("build", "node_modules", "website")
