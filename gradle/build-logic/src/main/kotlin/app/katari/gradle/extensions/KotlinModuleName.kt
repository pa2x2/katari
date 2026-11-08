package app.katari.gradle.extensions

import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Internal members compile to public JVM methods suffixed with the Kotlin module name, and published extensions link
 * against them. A module moving from an Android library to multiplatform keeps the name its Android build used (the
 * multiplatform default is derived from group and project) on every target, so converted extensions on desktop find
 * the same names.
 */
fun KotlinMultiplatformExtension.keepKotlinModuleName(name: String) {
    targets.configureEach {
        compilations.named { it == KotlinCompilation.MAIN_COMPILATION_NAME }.configureEach {
            compileTaskProvider.configure {
                (compilerOptions as? KotlinJvmCompilerOptions)?.moduleName?.set(name)
            }
        }
    }
}
