package tachiyomi.i18n.platform

import dev.icerock.moko.resources.StringResource
import mihon.core.platform.PlatformContext

expect fun PlatformContext.stringResource(resource: StringResource): String

expect fun PlatformContext.stringResource(resource: StringResource, vararg args: Any): String
