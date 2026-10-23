package mihon.gradle.extensionabi

import japicmp.cmp.JApiCmpArchive
import japicmp.cmp.JarArchiveComparator
import japicmp.cmp.JarArchiveComparatorOptions
import japicmp.config.Options
import japicmp.model.AccessModifier
import japicmp.model.JApiCompatibilityChangeType
import japicmp.model.JApiSemanticVersionLevel
import japicmp.output.OutputFilter
import japicmp.output.incompatible.IncompatibleErrorOutput
import japicmp.output.stdout.StdoutOutputGenerator
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import java.util.Optional

/**
 * The japicmp command line's `-a protected --include-synthetic --only-incompatible --ignore-missing-classes
 * --error-on-binary-incompatibility` run, through its API because only the API can reclassify change types.
 */
abstract class ExtensionAbiComparison : WorkAction<ExtensionAbiComparison.Parameters> {

    interface Parameters : WorkParameters {
        val baselineName: Property<String>
        val baselineJar: RegularFileProperty
        val currentJars: ConfigurableFileCollection
        val includes: ListProperty<String>
        val excludes: ListProperty<String>
        val compatibleChangeTypes: SetProperty<String>
        val report: RegularFileProperty
    }

    override fun execute() {
        val options = Options.newDefault().apply {
            oldArchives = listOf(JApiCmpArchive(parameters.baselineJar.get().asFile, parameters.baselineName.get()))
            newArchives = parameters.currentJars.map { JApiCmpArchive(it, "current") }
            setAccessModifier(AccessModifier.PROTECTED)
            isIncludeSynthetic = true
            isOutputOnlyBinaryIncompatibleModifications = true
            isErrorOnBinaryIncompatibility = true
            setIgnoreMissingClasses(true)
            addIncludeFromArgument(Optional.of(parameters.includes.get().joinToString(";")), false)
            parameters.excludes.get().takeIf { it.isNotEmpty() }?.let { excludes ->
                addExcludeFromArgument(Optional.of(excludes.joinToString(";")), false)
            }
        }
        val comparatorOptions = JarArchiveComparatorOptions.of(options)
        parameters.compatibleChangeTypes.get().forEach { type ->
            comparatorOptions.addOverrideCompatibilityChange(
                JarArchiveComparatorOptions.OverrideCompatibilityChange(
                    JApiCompatibilityChangeType.valueOf(type),
                    true,
                    true,
                    JApiSemanticVersionLevel.PATCH,
                ),
            )
        }

        val comparator = JarArchiveComparator(comparatorOptions)
        val classes = comparator.compare(options.oldArchives, options.newArchives)
        OutputFilter(options).filter(classes)
        parameters.report.get().asFile.writeText(StdoutOutputGenerator(options, classes).generate())
        IncompatibleErrorOutput(options, classes, comparator).generate()
    }
}
