package mihon.book.api.compat

import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentImage
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentWhiteSpace
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BookApi25AbiCompatibilityTest {

    @Test
    fun `sdk 2_5 document model entry points remain linkable`() {
        assertLegacyDataClassAbi(
            type = BookDocumentStyle::class.java,
            properties = arrayOf(
                BookDocumentAlignment::class.java,
                BookDocumentWhiteSpace::class.java,
                Long::class.javaObjectType,
                Long::class.javaObjectType,
                BookDocumentBorder::class.java,
                Float::class.javaPrimitiveType!!,
                BookDocumentFontFamily::class.java,
                Float::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
            ),
            hasDefaultConstructor = true,
        )
        assertLegacySerializationConstructor(
            BookDocumentStyle::class.java,
            Int::class.javaPrimitiveType!!,
            BookDocumentAlignment::class.java,
            BookDocumentWhiteSpace::class.java,
            Long::class.javaObjectType,
            Long::class.javaObjectType,
            BookDocumentBorder::class.java,
            Float::class.javaPrimitiveType!!,
            BookDocumentFontFamily::class.java,
            Float::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
        )

        assertLegacyDataClassAbi(
            type = BookDocumentInlineStyle::class.java,
            properties = arrayOf(
                Long::class.javaObjectType,
                Long::class.javaObjectType,
                BookDocumentFontFamily::class.java,
                Float::class.javaObjectType,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!,
            ),
            hasDefaultConstructor = true,
        )
        assertLegacySerializationConstructor(
            BookDocumentInlineStyle::class.java,
            Int::class.javaPrimitiveType!!,
            Long::class.javaObjectType,
            Long::class.javaObjectType,
            BookDocumentFontFamily::class.java,
            Float::class.javaObjectType,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
            Boolean::class.javaPrimitiveType!!,
        )

        assertLegacyDataClassAbi(
            type = BookDocumentImage::class.java,
            properties = arrayOf(
                String::class.java,
                BookDocumentRichText::class.java,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
            ),
            hasDefaultConstructor = false,
        )
        assertLegacySerializationConstructor(
            BookDocumentImage::class.java,
            Int::class.javaPrimitiveType!!,
            String::class.java,
            BookDocumentRichText::class.java,
            Int::class.javaObjectType,
            Int::class.javaObjectType,
        )
    }

    private fun assertLegacyDataClassAbi(
        type: Class<*>,
        properties: Array<Class<*>>,
        hasDefaultConstructor: Boolean,
    ) {
        assertNotNull(type.getDeclaredConstructor(*properties))
        assertNotNull(type.getDeclaredMethod("copy", *properties))
        assertNotNull(
            type.getDeclaredMethod(
                "copy\$default",
                type,
                *properties,
                Int::class.javaPrimitiveType!!,
                Any::class.java,
            ),
        )
        if (hasDefaultConstructor) {
            assertNotNull(
                type.getDeclaredConstructor(
                    *properties,
                    Int::class.javaPrimitiveType!!,
                    Class.forName("kotlin.jvm.internal.DefaultConstructorMarker"),
                ),
            )
        }
        properties.forEachIndexed { index, property ->
            assertTrue(type.getDeclaredMethod("component${index + 1}").returnType == property)
        }
    }

    private fun assertLegacySerializationConstructor(type: Class<*>, vararg properties: Class<*>) {
        val constructor = assertNotNull(
            type.getDeclaredConstructor(
                *properties,
                Class.forName("kotlinx.serialization.internal.SerializationConstructorMarker"),
            ),
        )
        assertTrue(Modifier.isPublic(constructor.modifiers))
    }
}
