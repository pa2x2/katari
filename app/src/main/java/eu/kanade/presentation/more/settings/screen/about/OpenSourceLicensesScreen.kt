package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.list.rowGroupItems
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.i18n.stringResource

class OpenSourceLicensesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val resources = LocalResources.current
        val libraries by produceState<List<Library>?>(null) {
            value = withContext(Dispatchers.IO) {
                val json = resources.openRawResource(R.raw.aboutlibraries).bufferedReader().use {
                    it.readText()
                }
                Libs.Builder().withJson(json).build().libraries
            }
        }
        var shownLibrary by remember { mutableStateOf<Library?>(null) }

        Scaffold(
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.licenses),
                    navigateUp = navigator::pop,
                    scrollBehavior = scrollBehavior,
                )
            },
        ) { contentPadding ->
            val loaded = libraries
            if (loaded == null) {
                LoadingScreen(Modifier.padding(contentPadding))
            } else {
                ScrollbarLazyColumn(contentPadding = contentPadding) {
                    rowGroupItems(loaded, key = Library::uniqueId) { library ->
                        LibraryRow(library = library, onClick = { shownLibrary = library })
                    }
                }
            }
        }

        shownLibrary?.let { OpenSourceLicenseDialog(library = it, onDismissRequest = { shownLibrary = null }) }
    }

    @Composable
    private fun LibraryRow(library: Library, onClick: () -> Unit) {
        val authors = library.developers.mapNotNull { it.name }.ifEmpty { listOfNotNull(library.organization?.name) }
        SettingsRow(
            title = library.name,
            subtitle = (authors + library.licenses.map { it.name }).joinToString(" · ").ifEmpty { null },
            trailing = library.artifactVersion?.let { version ->
                { Text(version, role = TextRole.Supporting, tone = ContentTone.Secondary) }
            },
            onClick = onClick,
        )
    }
}
