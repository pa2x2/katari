package app.katari.desktop.application.classdata

import java.nio.file.Path
import kotlin.io.path.bufferedWriter
import kotlin.io.path.readText

/**
 * A class list, as the JVM records one with `-XX:DumpLoadedClassList` and builds an archive from with
 * `-XX:SharedClassListFile`: the classes the platform's and the app's class loaders loaded, by name, and the
 * directives the JVM recorded for them, such as the lambdas it linked and the constant pool entries it resolved. A
 * directive is read once the classes it names are, so lists merge in the order each was recorded: a class or directive
 * already listed keeps its place, and one that is new goes after all that are listed.
 *
 * The classes extensions' own class loaders loaded are left out. Their entries name each other by number, which a
 * merge would have to renumber, and they come from extension jars, which update apart from the app.
 */
internal class ClassList private constructor(
    // A class by its name, a directive by its whole line
    private val entries: Set<String>,
) {
    val classCount: Int = entries.count { !it.startsWith(DIRECTIVE) }

    operator fun plus(other: ClassList): ClassList = ClassList(entries + other.entries)

    fun write(file: Path) {
        file.bufferedWriter().use { out ->
            var id = 0
            entries.forEach { entry ->
                out.append(if (entry.startsWith(DIRECTIVE)) entry else "$entry id: ${id++}").append('\n')
            }
        }
    }

    companion object {
        val EMPTY = ClassList(emptySet())

        fun read(file: Path): ClassList = parse(file.readText())

        fun parse(text: String): ClassList {
            // A run still recording may have written its last line in part
            val complete = if (text.endsWith('\n')) text else text.substringBeforeLast('\n', "")
            val entries = LinkedHashSet<String>()
            complete.lineSequence().forEach { line ->
                when {
                    line.isBlank() || line.startsWith(COMMENT) -> Unit
                    line.startsWith(DIRECTIVE) -> entries += line
                    UNREGISTERED_SOURCE !in line -> entries += line.substringBefore(' ')
                }
            }
            return ClassList(entries)
        }

        private const val COMMENT = "#"
        private const val DIRECTIVE = "@"

        // Where a class of another class loader came from, which only the entries of such classes have
        private const val UNREGISTERED_SOURCE = " source: "
    }
}
