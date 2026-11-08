package eu.kanade.presentation.category

import androidx.compose.runtime.Composable
import app.katari.core.platform.PlatformContext
import tachiyomi.domain.category.model.Category
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource
import tachiyomi.presentation.core.i18n.stringResource

val Category.visualName: String
    @Composable
    get() = when {
        isSystemCategory -> stringResource(MR.strings.label_default)
        else -> name
    }

fun Category.visualName(context: PlatformContext): String =
    when {
        isSystemCategory -> context.stringResource(MR.strings.label_default)
        else -> name
    }
