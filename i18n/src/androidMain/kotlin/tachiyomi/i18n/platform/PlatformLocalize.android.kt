package tachiyomi.i18n.platform

import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import mihon.core.platform.PlatformContext
import tachiyomi.i18n.context.pluralStringResource as contextPluralStringResource
import tachiyomi.i18n.context.stringResource as contextStringResource

actual fun PlatformContext.stringResource(resource: StringResource): String = contextStringResource(resource)

actual fun PlatformContext.stringResource(resource: StringResource, vararg args: Any): String =
    contextStringResource(resource, *args)

actual fun PlatformContext.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String =
    contextPluralStringResource(resource, count, *args)
