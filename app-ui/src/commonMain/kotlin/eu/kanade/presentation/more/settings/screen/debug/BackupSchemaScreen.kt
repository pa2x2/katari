package eu.kanade.presentation.more.settings.screen.debug

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.ContentCopy
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.backup.models.Backup
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

class BackupSchemaScreen : Screen() {

    companion object {
        const val TITLE = "Backup file schema"
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Composable
    override fun Content() {
        val clipboard = LocalAppUiGraph.current.textClipboard
        val navigator = LocalNavigator.currentOrThrow

        val schema = remember { ProtoBufSchemaGenerator.generateSchemaText(Backup.serializer().descriptor) }

        Scaffold(
            topBar = {
                AppBar(
                    title = TITLE,
                    navigateUp = navigator::pop,
                    actions = {
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_copy_to_clipboard),
                                    icon = MaterialSymbols.Rounded.ContentCopy,
                                    onClick = {
                                        clipboard.copy(TITLE, schema)
                                    },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            Text(
                text = schema,
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .padding(KatariTheme.spacing.l),
                role = TextRole.Code,
            )
        }
    }
}
