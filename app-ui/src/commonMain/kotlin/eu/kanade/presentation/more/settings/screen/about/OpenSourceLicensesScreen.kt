package eu.kanade.presentation.more.settings.screen.about

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.SettingsRow
import app.katari.presentation.kit.list.rowGroupItems
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.entity.Library
import eu.kanade.presentation.util.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class OpenSourceLicensesScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val definitions = LocalAppUiGraph.current.openSourceLibraryDefinitions
        val libraries by produceState<List<Library>?>(null) {
            value = withContext(Dispatchers.IO) {
                val json = definitions.readJson()
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
                RowList(contentPadding = contentPadding) {
                    rowGroupItems(loaded, key = Library::uniqueId) { library ->
                        LibraryRow(library = library, onClick = { shownLibrary = library })
                    }
                }
            }
        }

        shownLibrary?.let { OpenSourceLicenseSheet(library = it, onDismissRequest = { shownLibrary = null }) }
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
