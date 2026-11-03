package mihon.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class GenerateLocalesConfigTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val localeFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun action() {
        val locales = localeResourceTags(localeFiles)
            .joinToString("\n") { "|   <locale android:name=\"$it\"/>" }

        val content = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
        $locales
        |</locale-config>
        """.trimMargin()

        outputDir.get().file("xml/locales_config.xml").asFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    }
}
