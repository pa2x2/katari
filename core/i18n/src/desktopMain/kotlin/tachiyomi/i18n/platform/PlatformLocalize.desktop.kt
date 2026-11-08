package tachiyomi.i18n.platform

import app.katari.core.platform.PlatformContext
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.localized.pluralStringResource as localizedPluralStringResource
import tachiyomi.i18n.localized.stringResource as localizedStringResource

actual fun PlatformContext.stringResource(resource: StringResource): String = localizedStringResource(resource)

actual fun PlatformContext.stringResource(resource: StringResource, vararg args: Any): String =
    localizedStringResource(resource, *args)

actual fun PlatformContext.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String =
    localizedPluralStringResource(resource, count, *args)
