package tachiyomi.presentation.core.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import dev.icerock.moko.resources.PluralsResource
import dev.icerock.moko.resources.StringResource

@Composable
@ReadOnlyComposable
expect fun stringResource(resource: StringResource): String

@Composable
@ReadOnlyComposable
expect fun stringResource(resource: StringResource, vararg args: Any): String

@Composable
@ReadOnlyComposable
expect fun pluralStringResource(resource: PluralsResource, count: Int): String

@Composable
@ReadOnlyComposable
expect fun pluralStringResource(resource: PluralsResource, count: Int, vararg args: Any): String
