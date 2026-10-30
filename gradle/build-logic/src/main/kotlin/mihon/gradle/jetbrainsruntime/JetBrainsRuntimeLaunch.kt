package mihon.gradle.jetbrainsruntime

import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.withType
import org.jetbrains.compose.desktop.application.dsl.JvmApplication

/**
 * Starts [task] on the pinned JetBrains Runtime, made ready first, through the launcher Gradle's `JavaExec` lets a build
 * pick; with `-PsystemJdk` it stays on the JDK running Gradle.
 */
fun Project.launchOnJetBrainsRuntime(task: TaskProvider<out JavaExec>) {
    if (runsOnSystemJdk) return
    val runtime = jetBrainsRuntime()
    val provision = provisionJetBrainsRuntime(runtime)
    val launcher = launcher(runtime)
    task.configure {
        dependsOn(provision)
        javaLauncher.set(launcher)
    }
}

/**
 * Starts [application]'s run tasks on the pinned JetBrains Runtime: Compose's `javaHome` makes it each run task's
 * executable, and the task's launcher, which Gradle starts the task with, is the same runtime. With `-PsystemJdk` they
 * stay on the JDK running Gradle, Compose's default. Compose's packaging tasks read `javaHome` too and need a full JDK,
 * which this runtime isn't; the app has no package yet.
 */
internal fun Project.runOnJetBrainsRuntime(application: JvmApplication) {
    if (runsOnSystemJdk) return
    val runtime = jetBrainsRuntime()
    val provision = provisionJetBrainsRuntime(runtime)
    application.javaHome = runtime.home.absolutePath
    // JCEF is a module of the runtime, so the app's own grant to the classpath doesn't cover its loading of Chromium
    application.jvmArgs += "--enable-native-access=jcef"
    val launcher = launcher(runtime)
    tasks.withType<JavaExec>().configureEach {
        dependsOn(provision)
        javaLauncher.set(launcher)
    }
}

private fun Project.launcher(runtime: JetBrainsRuntime) = JetBrainsRuntimeLauncher(
    home = layout.projectDirectory.dir(runtime.home.absolutePath),
    version = runtime.version,
    build = runtime.build,
)
