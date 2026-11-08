package tachiyomi.i18n.platform

import app.katari.core.platform.PlatformContext
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource

expect fun PlatformContext.stringResource(resource: StringResource): String

expect fun PlatformContext.stringResource(resource: StringResource, vararg args: Any): String

expect fun PlatformContext.pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String
