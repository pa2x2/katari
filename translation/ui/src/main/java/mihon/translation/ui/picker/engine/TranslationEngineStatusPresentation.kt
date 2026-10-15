package mihon.translation.ui.picker.engine

import mihon.presentation.kit.surface.StatusTone
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineBuildAvailability
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.engine.TranslationEngineState
import mihon.translation.api.engine.TranslationEngineStatus
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason

internal enum class TranslationEngineStatusLabel {
    Checking,
    Ready,
    NotInstalled,
    ConfigurationRequired,
    ProviderDisclosureRequired,
    ModelDownloadRequired,
    SystemSetupRequired,
    SetupInProgress,
    Unavailable,
}

internal sealed interface TranslationEngineStatusExplanation {
    data object InstallProvider : TranslationEngineStatusExplanation

    data object ProviderDisclosure : TranslationEngineStatusExplanation

    data object LanguageData : TranslationEngineStatusExplanation

    data class SystemSetup(
        val reason: TranslationSystemSetupReason,
    ) : TranslationEngineStatusExplanation

    data class Unavailable(
        val reason: TranslationUnavailableReason,
        val buildReason: String?,
    ) : TranslationEngineStatusExplanation

    data class ProviderText(
        val text: String,
    ) : TranslationEngineStatusExplanation
}

internal data class TranslationEngineStatusPresentation(
    val label: TranslationEngineStatusLabel,
    val tone: StatusTone,
    val explanation: TranslationEngineStatusExplanation?,
)

internal data class TranslationEngineCardModel(
    val state: TranslationEngineState,
    val selected: Boolean,
    val selectable: Boolean,
    val status: TranslationEngineStatusPresentation,
    val action: TranslationEngineAction?,
)

internal data class TranslationEnginePickerModel(
    val cards: List<TranslationEngineCardModel>,
)

internal fun projectTranslationEnginePicker(
    states: List<TranslationEngineState>,
    selectedEngine: TranslationEngineId?,
): TranslationEnginePickerModel {
    return TranslationEnginePickerModel(
        cards = states.map { projectTranslationEngineCard(it, selectedEngine) },
    )
}

internal fun isTranslationEngineSelectionMissing(
    states: List<TranslationEngineState>,
    selectedEngine: TranslationEngineId?,
): Boolean = selectedEngine != null && states.none { it.engine.id == selectedEngine }

internal fun projectTranslationEngineCard(
    state: TranslationEngineState,
    selectedEngine: TranslationEngineId?,
): TranslationEngineCardModel {
    return TranslationEngineCardModel(
        state = state,
        selected = state.engine.id == selectedEngine,
        selectable = state.status == TranslationEngineStatus.Ready,
        status = state.status.toPresentation(state.engine.buildAvailability),
        action = state.action,
    )
}

private fun TranslationEngineStatus.toPresentation(
    buildAvailability: TranslationEngineBuildAvailability,
): TranslationEngineStatusPresentation {
    return when (this) {
        TranslationEngineStatus.Checking -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.Checking,
            tone = StatusTone.Checking,
            explanation = null,
        )
        TranslationEngineStatus.Ready -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.Ready,
            tone = StatusTone.Ready,
            explanation = null,
        )
        TranslationEngineStatus.NotInstalled -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.NotInstalled,
            tone = StatusTone.ActionRequired,
            explanation = TranslationEngineStatusExplanation.InstallProvider,
        )
        is TranslationEngineStatus.ConfigurationRequired -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.ConfigurationRequired,
            tone = StatusTone.ActionRequired,
            explanation = TranslationEngineStatusExplanation.ProviderText(reason),
        )
        is TranslationEngineStatus.ProviderDisclosureRequired -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.ProviderDisclosureRequired,
            tone = StatusTone.ActionRequired,
            explanation = TranslationEngineStatusExplanation.ProviderDisclosure,
        )
        is TranslationEngineStatus.ModelDownloadRequired -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.ModelDownloadRequired,
            tone = StatusTone.ActionRequired,
            explanation = TranslationEngineStatusExplanation.LanguageData,
        )
        is TranslationEngineStatus.SystemSetupRequired -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.SystemSetupRequired,
            tone = StatusTone.ActionRequired,
            explanation = TranslationEngineStatusExplanation.SystemSetup(reason),
        )
        is TranslationEngineStatus.SetupInProgress -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.SetupInProgress,
            tone = StatusTone.Checking,
            explanation = null,
        )
        is TranslationEngineStatus.Unavailable -> TranslationEngineStatusPresentation(
            label = TranslationEngineStatusLabel.Unavailable,
            tone = StatusTone.Unavailable,
            explanation = TranslationEngineStatusExplanation.Unavailable(
                reason = reason,
                buildReason = (buildAvailability as? TranslationEngineBuildAvailability.NotIncluded)?.reason,
            ),
        )
    }
}
