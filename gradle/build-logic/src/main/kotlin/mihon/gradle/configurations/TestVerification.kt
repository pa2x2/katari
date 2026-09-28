package mihon.gradle.configurations

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasHostTests
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

/**
 * Lifecycle task that every module registers, so running it by name covers every module's tests
 * without a hand-maintained list of test tasks that new modules and test source sets fall out of.
 */
const val VERIFY_TESTS_TASK_NAME = "verifyTests"

/**
 * Runs the host tests of every variant AGP creates them for and compiles its device tests, which
 * cannot run without a device but must keep compiling against the code they exercise.
 */
fun Project.configureAndroidTestVerification() {
    val verifyTests = registerTestVerification()
    extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
        val hostTestTasks = (variant as? HasHostTests)?.hostTests.orEmpty().values
            .map { "test${it.name.replaceFirstChar(Char::uppercase)}" }
        val deviceTestCompileTasks = (variant as? HasDeviceTests)?.deviceTests.orEmpty().values
            .map { "compile${it.name.replaceFirstChar(Char::uppercase)}Kotlin" }
        verifyTests.configure { dependsOn(hostTestTasks + deviceTestCompileTasks) }
    }
}

fun Project.configureMultiplatformTestVerification() {
    registerTestVerification().configure { dependsOn("allTests") }
}

private fun Project.registerTestVerification(): TaskProvider<Task> {
    return tasks.register(VERIFY_TESTS_TASK_NAME) {
        group = "verification"
        description = "Runs this module's host tests and compiles its device tests"
    }
}
