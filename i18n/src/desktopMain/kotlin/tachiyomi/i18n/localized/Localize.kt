package tachiyomi.i18n.localized

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Plural
import dev.icerock.moko.resources.desc.PluralFormatted
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import tachiyomi.i18n.unescapeQuotes

// The desktop counterparts of tachiyomi.i18n.context, in the JVM's default locale

fun stringResource(resource: StringResource): String {
    return StringDesc.Resource(resource).localized().unescapeQuotes()
}

fun stringResource(resource: StringResource, vararg args: Any): String {
    return StringDesc.ResourceFormatted(resource, *args).localized().unescapeQuotes()
}

fun pluralStringResource(resource: PluralsResource, count: Int): String {
    return StringDesc.Plural(resource, count).localized().unescapeQuotes()
}

fun pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String {
    return StringDesc.PluralFormatted(resource, count, *args).localized().unescapeQuotes()
}
