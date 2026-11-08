package app.katari.extension.runtime.jvm.apk

import app.katari.extension.runtime.jvm.apk.binary.BinaryXml
import app.katari.extension.runtime.jvm.apk.binary.ResourceValue

/**
 * What a package manager reads from an apk's compiled `AndroidManifest.xml`: its identity, the features it uses, and
 * its application's label, icon and meta-data, each value with the type `aapt` gave it.
 *
 * @property versionCode the long version code Android reads: `versionCodeMajor` in the high 32 bits.
 * @property metaData the application's `<meta-data>` by name in manifest order, `null` when it has none, as Android
 * leaves an application without any with no meta-data bundle.
 */
internal class ApkManifest(
    val packageName: String,
    val versionCode: Long,
    val versionName: String?,
    val features: Set<String>,
    val label: BinaryXml.Attribute?,
    val applicationName: String?,
    val iconResourceId: Int,
    val metaData: List<MetaDataEntry>?,
) {
    /**
     * A `<meta-data>` element: its `android:resource`'s ID when it names one, otherwise its `android:value`, which
     * may be a reference that the apk's resources resolve.
     */
    data class MetaDataEntry(val name: String, val resourceId: Int, val value: BinaryXml.Attribute?)

    companion object {
        private const val ATTR_LABEL = 0x01010001
        private const val ATTR_ICON = 0x01010002
        private const val ATTR_NAME = 0x01010003
        private const val ATTR_VALUE = 0x01010024
        private const val ATTR_RESOURCE = 0x01010025
        private const val ATTR_VERSION_CODE = 0x0101021b
        private const val ATTR_VERSION_NAME = 0x0101021c
        private const val ATTR_VERSION_CODE_MAJOR = 0x01010576

        fun parse(bytes: ByteArray): ApkManifest {
            val elements = BinaryXml(bytes).elements
            val manifest = elements.firstOrNull()?.takeIf { it.name == "manifest" && it.depth == 1 }
                ?: throw IllegalArgumentException("The manifest has no <manifest> element")
            val packageName = manifest.plainAttribute("package")?.string
                ?: throw IllegalArgumentException("The manifest names no package")

            // Only the manifest's own children and the application's: an activity's meta-data isn't the application's
            var inApplication = false
            val features = mutableSetOf<String>()
            var application: BinaryXml.Element? = null
            var metaData: MutableList<MetaDataEntry>? = null
            for (element in elements.drop(1)) {
                if (element.depth <= 2) inApplication = false
                when {
                    element.depth == 2 && element.name == "uses-feature" ->
                        element.attribute(ATTR_NAME, "name")?.string?.let(features::add)
                    element.depth == 2 && element.name == "application" && application == null -> {
                        application = element
                        inApplication = true
                    }
                    inApplication && element.depth == 3 && element.name == "meta-data" -> {
                        val name = element.attribute(ATTR_NAME, "name")?.string ?: continue
                        val resource = element.attribute(ATTR_RESOURCE, "resource")
                            ?.value
                            ?.takeIf { it.type == ResourceValue.REFERENCE }
                            ?.data
                            ?: 0
                        val entry = MetaDataEntry(name, resource, element.attribute(ATTR_VALUE, "value"))
                        (metaData ?: mutableListOf<MetaDataEntry>().also { metaData = it }).add(entry)
                    }
                }
            }

            val versionCode = manifest.attribute(ATTR_VERSION_CODE, "versionCode")?.value?.intOrNull() ?: 0
            val versionCodeMajor = manifest.attribute(ATTR_VERSION_CODE_MAJOR, "versionCodeMajor")?.value?.intOrNull()
                ?: 0
            return ApkManifest(
                packageName = packageName,
                versionCode = (versionCodeMajor.toLong() shl 32) or (versionCode.toLong() and 0xffffffffL),
                versionName = manifest.attribute(ATTR_VERSION_NAME, "versionName")?.let { it.string ?: it.intText() },
                features = features,
                label = application?.attribute(ATTR_LABEL, "label"),
                applicationName = application?.attribute(ATTR_NAME, "name")?.string?.let { className(packageName, it) },
                iconResourceId = application?.attribute(ATTR_ICON, "icon")
                    ?.value
                    ?.takeIf { it.type == ResourceValue.REFERENCE }
                    ?.data
                    ?: 0,
                metaData = metaData,
            )
        }

        private fun ResourceValue.intOrNull(): Int? = data.takeIf { isInt }

        /** A version name `aapt` compiled to a number, such as `1`, read back as the text it was. */
        private fun BinaryXml.Attribute.intText(): String? = value.intOrNull()?.toString()

        /** A class name the manifest gives relative to the package, as Android completes it. */
        private fun className(packageName: String, name: String): String = when {
            name.startsWith(".") -> packageName + name
            '.' !in name -> "$packageName.$name"
            else -> name
        }
    }
}
