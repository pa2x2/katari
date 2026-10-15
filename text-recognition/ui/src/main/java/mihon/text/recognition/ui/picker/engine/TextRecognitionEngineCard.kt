package mihon.text.recognition.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.surface.Pill
import mihon.presentation.kit.surface.SelectableCard
import mihon.presentation.kit.surface.StatusPill
import mihon.presentation.kit.surface.StatusTone
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.settings.isIncluded
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** An engine as a choice in the picker; an engine this build excludes is shown but cannot be chosen. */
@Composable
internal fun TextRecognitionEngineCard(
    provider: KnownTextRecognitionProvider,
    components: List<KnownTextRecognitionComponent>,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val spacing = KatariTheme.spacing
    val included = provider.isIncluded
    SelectableCard(selected = selected, onClick = onSelect, enabled = included) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(provider.name, role = TextRole.SectionTitle)
                Text(provider.description, role = TextRole.Body, tone = ContentTone.Secondary)
            }
            if (included) {
                RadioButton(selected = selected, onClick = null)
            } else {
                StatusPill(
                    text = stringResource(MR.strings.text_recognition_engine_status_not_included),
                    tone = StatusTone.Unavailable,
                )
            }
        }
        when (val availability = provider.buildAvailability) {
            TextRecognitionBuildAvailability.Included -> {
                provider.bestFor?.let {
                    Text(stringResource(MR.strings.text_recognition_engine_best_for, it), role = TextRole.Body)
                }
                val labels = engineLanguageLabels(components)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.s),
                    verticalArrangement = Arrangement.spacedBy(spacing.xs),
                ) {
                    labels.take(SHOWN_LANGUAGE_LABELS).forEach { LanguagePill(it) }
                    val more = labels.size - SHOWN_LANGUAGE_LABELS
                    if (more > 0) {
                        LanguagePill(stringResource(MR.strings.text_recognition_engine_more_scripts, more))
                    }
                }
            }
            is TextRecognitionBuildAvailability.NotIncluded -> Text(
                text = availability.reason,
                role = TextRole.Body,
                tone = ContentTone.Secondary,
            )
        }
        Button(
            label = stringResource(MR.strings.text_recognition_engine_details),
            onClick = onOpenDetails,
            style = ButtonStyle.Text,
        )
    }
}

/**
 * What an engine reads, one label per script: the language's name for a single-language script, otherwise the
 * script's name and how many languages it covers. Languages no script groups are named on their own.
 */
@Composable
private fun engineLanguageLabels(components: List<KnownTextRecognitionComponent>): List<String> {
    val scripts = components.flatMap { it.scripts }.distinctBy { it.displayName }
    val grouped = scripts.flatMapTo(mutableSetOf()) { it.languages }
    val ungrouped = components.flatMap { it.languages }.distinct().filterNot { it in grouped }
    return scripts.map { script ->
        script.languages.singleOrNull()?.displayName()
            ?: stringResource(
                MR.strings.text_recognition_engine_script_languages,
                script.displayName,
                script.languages.size,
            )
    } + ungrouped.map { it.displayName() }
}

@Composable
private fun LanguagePill(label: String) {
    Pill(text = label, container = SurfaceContainer.SecondaryContainer)
}

private const val SHOWN_LANGUAGE_LABELS = 6
