package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import com.mikepenz.aboutlibraries.entity.Library
import mihon.presentation.kit.sheet.DetailsLink
import mihon.presentation.kit.sheet.DetailsSection
import mihon.presentation.kit.sheet.DetailsSheet
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A library's licence texts, one section per licence. A licence whose generated data carries no text shows only its
 * name, and the library's website stays one tap away.
 */
@Composable
internal fun OpenSourceLicenseSheet(library: Library, onDismissRequest: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val website = library.website ?: library.scm?.url
    DetailsSheet(
        title = library.name,
        onDismissRequest = onDismissRequest,
        subtitle = library.artifactVersion,
        links = listOfNotNull(
            website?.let { DetailsLink(stringResource(MR.strings.website), { uriHandler.openUri(it) }) },
        ),
    ) {
        library.licenses.forEach { license ->
            val text = license.licenseContent?.takeIf(String::isNotBlank)
            if (text == null) DetailsSection(license.name) {} else DetailsSection(license.name, text)
        }
    }
}
