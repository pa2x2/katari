package mihon.extension.runtime.jvm.apk.binary

/**
 * The elements of a compiled XML file such as an apk's `AndroidManifest.xml`, in document order, with their attributes'
 * typed values as `aapt` wrote them. Text and namespaces aren't kept: nothing read from a manifest needs them.
 */
internal class BinaryXml(bytes: ByteArray) {

    /** [depth] is 1 for the root element. */
    data class Element(val name: String, val depth: Int, val attributes: List<Attribute>) {

        /**
         * The attribute with [resourceId] or, without one, named [name]: `aapt` may drop the names of the platform's
         * attributes, which Android looks up by ID.
         */
        fun attribute(resourceId: Int, name: String): Attribute? =
            attributes.firstOrNull { it.resourceId == resourceId } ?: attributes.firstOrNull { it.name == name }

        /** The attribute named [name] in no namespace, such as the manifest's `package`. */
        fun plainAttribute(name: String): Attribute? =
            attributes.firstOrNull { it.name == name && it.resourceId == 0 && it.namespace == null }
    }

    /** [string] is the text of a [ResourceValue.STRING] value, `null` for the other types. */
    data class Attribute(
        val namespace: String?,
        val name: String,
        val resourceId: Int,
        val value: ResourceValue,
        val string: String?,
    )

    val elements: List<Element>

    init {
        val root = ResourceChunk.of(bytes)
        require(root.type == ResourceChunk.XML) { "Not a compiled XML file" }
        var strings: ResourceStringPool? = null
        var resourceIds = IntArray(0)
        val elements = mutableListOf<Element>()
        var depth = 0
        for (chunk in root.children()) {
            when (chunk.type) {
                ResourceChunk.STRING_POOL -> strings = ResourceStringPool(chunk)
                ResourceChunk.XML_RESOURCE_MAP ->
                    resourceIds =
                        IntArray((chunk.size - chunk.headerSize) / 4) { chunk.s32(chunk.headerSize + it * 4) }
                ResourceChunk.XML_START_ELEMENT -> {
                    val pool = checkNotNull(strings) { "An element comes before the string pool" }
                    depth++
                    elements += element(chunk, pool, resourceIds, depth)
                }
                ResourceChunk.XML_END_ELEMENT -> depth--
            }
        }
        this.elements = elements
    }

    private fun element(chunk: ResourceChunk, strings: ResourceStringPool, resourceIds: IntArray, depth: Int): Element {
        val ext = chunk.headerSize
        val attributeStart = ext + chunk.u16(ext + 8)
        val attributeSize = chunk.u16(ext + 10)
        val attributeCount = chunk.u16(ext + 12)
        val attributes = List(attributeCount) { index ->
            val at = attributeStart + index * attributeSize
            val nameIndex = chunk.u32(at + 4)
            val value = ResourceValue.at(chunk, at + 12)
            Attribute(
                namespace = strings[chunk.u32(at)],
                name = strings[nameIndex].orEmpty(),
                resourceId = resourceIds.getOrElse(nameIndex.toInt()) { 0 },
                value = value,
                string = if (value.type == ResourceValue.STRING) strings[value.data.toLong() and 0xffffffffL] else null,
            )
        }
        return Element(name = strings[chunk.u32(ext + 4)].orEmpty(), depth = depth, attributes = attributes)
    }
}
