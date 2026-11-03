package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.localized.pluralStringResource as localizedPluralString
import tachiyomi.i18n.localized.stringResource as localizedString

@Composable
@ReadOnlyComposable
actual fun stringResource(resource: StringResource): String {
    return localizedString(resource)
}

@Composable
@ReadOnlyComposable
actual fun stringResource(resource: StringResource, vararg args: Any): String {
    return localizedString(resource, *args)
}

@Composable
@ReadOnlyComposable
actual fun pluralStringResource(resource: PluralsResource, count: Int): String {
    return localizedPluralString(resource, count)
}

@Composable
@ReadOnlyComposable
actual fun pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String {
    return localizedPluralString(resource, count, *args)
}
