package tachiyomi.i18n.context

import android.content.Context
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.desc.Plural
import dev.icerock.moko.resources.desc.PluralFormatted
import dev.icerock.moko.resources.desc.Resource
import dev.icerock.moko.resources.desc.ResourceFormatted
import dev.icerock.moko.resources.desc.StringDesc
import tachiyomi.i18n.unescapeQuotes

fun Context.stringResource(resource: StringResource): String {
    return StringDesc.Resource(resource).toString(this).unescapeQuotes()
}

fun Context.stringResource(resource: StringResource, vararg args: Any): String {
    return StringDesc.ResourceFormatted(resource, *args).toString(this).unescapeQuotes()
}

fun Context.pluralStringResource(resource: PluralsResource, count: Int): String {
    return StringDesc.Plural(resource, count).toString(this).unescapeQuotes()
}

fun Context.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String {
    return StringDesc.PluralFormatted(resource, count, *args).toString(this).unescapeQuotes()
}
