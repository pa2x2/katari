package tachiyomi.i18n.platform

import dev.icerock.moko.resources.StringResource
import mihon.core.platform.PlatformContext
import tachiyomi.i18n.localized.stringResource as localizedStringResource

actual fun PlatformContext.stringResource(resource: StringResource): String = localizedStringResource(resource)

actual fun PlatformContext.stringResource(resource: StringResource, vararg args: Any): String =
    localizedStringResource(resource, *args)
