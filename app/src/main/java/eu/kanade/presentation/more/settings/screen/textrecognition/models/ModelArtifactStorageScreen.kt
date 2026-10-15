package eu.kanade.presentation.more.settings.screen.textrecognition.models

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Screen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Delete
import mihon.model.artifacts.api.state.StoredModelArtifact
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.dialog.ConfirmDialog
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.list.SectionHeader
import mihon.presentation.kit.list.SettingsRow
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.Card
import mihon.presentation.kit.surface.CardStyle
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.ui.language.shortDisplayNames
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Lists every stored model revision, complete or partial, by what the profile reads with it, and lets the user
 * reclaim storage. Deleting a model the profile still reads with asks first.
 */
internal class ModelArtifactStorageScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = viewModel { ModelArtifactStorageViewModel() }
        val stored by model.stored.collectAsState()
        var confirming by remember { mutableStateOf<TextRecognitionStoredModels.InUse?>(null) }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.model_artifacts_title),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val models = stored
            when {
                models == null -> LoadingScreen(Modifier.padding(contentPadding))
                models.totalBytes == 0L -> EmptyScreen(
                    stringRes = MR.strings.model_artifacts_empty,
                    modifier = Modifier.padding(contentPadding),
                )
                else -> LazyColumn(contentPadding = contentPadding) {
                    item(key = "summary") { StorageSummary(models, onFree = model::deleteObsolete) }
                    section(MR.strings.text_recognition_models_in_use, models.inUse.map { it.artifact }) { artifact ->
                        val use = models.inUse.first { it.artifact == artifact }
                        StoredModelRow(artifact, usage = use.usageLabel(), onDelete = { confirming = use })
                    }
                    section(MR.strings.text_recognition_models_unused, models.unused) { artifact ->
                        StoredModelRow(artifact, usage = null, onDelete = { model.delete(artifact.descriptor.id) })
                    }
                    section(MR.strings.text_recognition_models_obsolete, models.obsolete) { artifact ->
                        StoredModelRow(
                            artifact = artifact,
                            usage = stringResource(MR.strings.text_recognition_models_obsolete_reason),
                            onDelete = { model.delete(artifact.descriptor.id) },
                        )
                    }
                }
            }
        }
        confirming?.let { use ->
            val size = formatModelArtifactSize(use.artifact.descriptor.sizeBytes)
            ConfirmDialog(
                title = stringResource(
                    MR.strings.text_recognition_models_delete_title,
                    use.artifact.descriptor.displayName,
                ),
                text = if (use.everyLanguage) {
                    stringResource(MR.strings.text_recognition_models_delete_message_every_language, size)
                } else {
                    stringResource(
                        MR.strings.text_recognition_models_delete_message,
                        use.languages.shortDisplayNames(),
                        size,
                    )
                },
                confirmLabel = stringResource(MR.strings.action_delete),
                onConfirm = {
                    confirming = null
                    model.delete(use.artifact.descriptor.id)
                },
                onDismissRequest = { confirming = null },
                destructive = true,
            )
        }
    }
}

private fun LazyListScope.section(
    title: StringResource,
    artifacts: List<StoredModelArtifact>,
    row: @Composable (StoredModelArtifact) -> Unit,
) {
    if (artifacts.isEmpty()) return
    item(key = "heading-${title.resourceId}") { SectionHeader(stringResource(title)) }
    items(artifacts, key = { "${it.descriptor.id.value}@${it.descriptor.revision}" }) { row(it) }
}

@Composable
private fun StorageSummary(models: TextRecognitionStoredModels, onFree: () -> Unit) {
    val count = models.inUse.size + models.unused.size + models.obsolete.size
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = KatariTheme.spacing.contentInset, vertical = KatariTheme.spacing.s),
        style = CardStyle.Elevated,
    ) {
        Row(
            modifier = Modifier.padding(KatariTheme.spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatModelArtifactSize(models.totalBytes), role = TextRole.Metric)
                Text(
                    text = pluralStringResource(MR.plurals.model_artifacts_count, count, count),
                    role = TextRole.Body,
                    tone = ContentTone.Secondary,
                )
            }
            if (models.obsolete.isNotEmpty()) {
                Button(
                    label = stringResource(
                        MR.strings.model_artifacts_free,
                        formatModelArtifactSize(models.obsoleteBytes),
                    ),
                    onClick = onFree,
                    style = ButtonStyle.Tonal,
                )
            }
        }
    }
}

@Composable
private fun StoredModelRow(artifact: StoredModelArtifact, usage: String?, onDelete: () -> Unit) {
    val size = formatModelArtifactSize(artifact.storedBytes)
    val state = if (artifact.complete) {
        stringResource(MR.strings.model_artifacts_state_installed, size)
    } else {
        stringResource(MR.strings.model_artifacts_state_partial, size)
    }
    SettingsRow(
        title = artifact.descriptor.displayName,
        subtitle = listOfNotNull(state, usage).joinToString(" · "),
        trailing = {
            IconButton(
                icon = MaterialSymbols.Rounded.Delete,
                contentDescription = stringResource(MR.strings.action_delete),
                onClick = onDelete,
            )
        },
    )
}

@Composable
private fun TextRecognitionStoredModels.InUse.usageLabel(): String =
    if (everyLanguage) {
        stringResource(MR.strings.text_recognition_models_used_for_every_language)
    } else {
        stringResource(MR.strings.text_recognition_models_used_for, languages.shortDisplayNames())
    }
