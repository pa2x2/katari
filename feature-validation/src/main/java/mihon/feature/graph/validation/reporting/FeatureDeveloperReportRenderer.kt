package mihon.feature.graph.validation.reporting

fun renderFeatureDeveloperReport(report: FeatureDeveloperReport): String = buildString {
    appendLine("Katari feature developer report")
    appendLine("================================")
    appendLine(
        "Content types: ${report.contentTypes.size}; features: ${report.features.size}; " +
            "execution points: ${report.executionPoints.size}; " +
            "evaluated integrations: ${report.integrations.size}; obligations: ${report.obligations.size}",
    )
    appendLine(
        "Contextual validation scenarios are samples of conditional relationships; " +
            "they never establish type-wide support.",
    )

    appendLine()
    appendLine("Content types")
    appendLine("-------------")
    report.contentTypes.forEach { contentType ->
        appendLine("- ${contentType.id} (owner: ${contentType.owner})")
        appendReferences("providers", contentType.providers)
        appendReferences("specialized adapters", contentType.specializedAdapters)
        appendReferences("contract fixtures", contentType.contractFixtures)
    }

    appendLine()
    appendLine("Features")
    appendLine("--------")
    report.features.forEach { feature ->
        appendLine("- ${feature.id} (owner: ${feature.owner})")
    }

    appendLine()
    appendLine("Execution points")
    appendLine("----------------")
    if (report.executionPoints.isEmpty()) {
        appendLine("- none")
    } else {
        report.executionPoints.forEach { point ->
            appendLine(
                "- ${point.id} (owner: ${point.owner}; event: ${point.eventType}; " +
                    "phase: ${point.phase}; failure: ${point.failurePolicy})",
            )
        }
    }

    appendLine()
    appendLine("Evaluated execution participants")
    appendLine("--------------------------------")
    if (report.executionParticipants.isEmpty()) {
        appendLine("- none")
    } else {
        report.executionParticipants.forEach { participant ->
            appendLine(
                "- ${participant.contentType.id} -> ${participant.point.id}/${participant.participant.id} " +
                    "[${participant.state.name.lowercase()}]",
            )
            appendLine(
                "  owners: type=${participant.contentType.owner}; point=${participant.point.owner}; " +
                    "participant=${participant.participant.owner}",
            )
            appendLine("  prerequisites: ${participant.prerequisites.render()}")
            appendReferences("context inputs", participant.contextInputs, indent = "  ")
            if (participant.after.isNotEmpty()) appendLine("  after: ${participant.after.joinToString()}")
            if (participant.before.isNotEmpty()) appendLine("  before: ${participant.before.joinToString()}")
            if (participant.contracts.isNotEmpty()) {
                appendLine("  contracts:")
                participant.contracts.forEach { contract ->
                    appendLine("    - ${contract.id}")
                    contract.validations.forEach { validation ->
                        appendContractValidation(validation, indent = "      ")
                    }
                }
            }
        }
    }

    appendLine()
    appendLine("Evaluated integrations")
    appendLine("----------------------")
    report.integrations.forEach { integration ->
        appendLine(
            "- ${integration.contentType.id} -> ${integration.feature.id}/${integration.id} " +
                "[${integration.state.name.lowercase()}]",
        )
        appendLine("  owners: type=${integration.contentType.owner}; feature=${integration.feature.owner}")
        appendLine("  prerequisites: ${integration.prerequisites.render()}")
        appendReferences("matched providers", integration.matchedProviders, indent = "  ")
        appendRequirements("unmet prerequisites", integration.unmetPrerequisites)
        appendReferences(
            "missing specialized prerequisites",
            integration.missingSpecializedPrerequisites,
            indent = "  ",
        )
        appendReferences("supplied specialized adapters", integration.suppliedSpecializedAdapters, indent = "  ")
        appendReferences(
            "pending specialized requirements",
            integration.pendingSpecializedRequirements,
            indent = "  ",
        )
        appendReferences("context inputs", integration.contextInputs, indent = "  ")
        if (integration.declaredBlockers.isNotEmpty()) {
            appendLine(
                "  possible blockers: " + integration.declaredBlockers.joinToString { blocker ->
                    "${blocker.id} (${blocker.inputs.joinToString { it.id }})"
                },
            )
        }
        appendArtifacts("behaviors", integration.behaviors)
        if (integration.contracts.isNotEmpty()) {
            appendLine("  contracts:")
            integration.contracts.forEach { contract ->
                appendLine("    - ${contract.id} [${contract.availability.name.lowercase()}]")
                appendReferences("fixtures", contract.fixtureRequirements, indent = "      ")
                contract.validations.forEach { validation ->
                    appendContractValidation(validation, indent = "      ")
                }
            }
        }
        if (integration.projections.isNotEmpty()) {
            appendLine("  projections:")
            integration.projections.forEach { projection ->
                appendLine(
                    "    - ${projection.id} [${projection.availability.name.lowercase()}]; " +
                        "owner=${projection.owner}; implementation=${projection.implementationPresent}",
                )
            }
        }
    }

    appendLine()
    appendLine("Obligations")
    appendLine("-----------")
    if (report.obligations.isEmpty()) {
        appendLine("- none")
    } else {
        report.obligations.forEach { obligation ->
            val subjects = obligation.subjects.joinToString { subject ->
                "${subject.contentType}:${subject.feature}:${subject.integration}"
            }
            appendLine(
                "- ${obligation.category.name.lowercase()}: ${obligation.artifact}; " +
                    "owner=${obligation.responsibleOwner}; subjects=$subjects",
            )
            appendLine("  ${obligation.details}")
        }
    }
}.trimEnd() + "\n"

private fun StringBuilder.appendReferences(
    label: String,
    references: List<FeatureDeveloperOwnedReference>,
    indent: String = "  ",
) {
    if (references.isNotEmpty()) {
        appendLine("$indent$label: ${references.joinToString { "${it.id} (${it.owner})" }}")
    }
}

private fun StringBuilder.appendContractValidation(
    validation: FeatureDeveloperContractValidation,
    indent: String,
) {
    val scenario = validation.scenario?.let { " scenario=$it" }.orEmpty()
    val details = validation.details.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "; details=", separator = " | ")
        .orEmpty()
    appendLine("${indent}validation:$scenario ${validation.outcome.name.lowercase()}$details")
}

private fun StringBuilder.appendRequirements(
    label: String,
    requirements: List<FeatureDeveloperCapabilityRequirement>,
) {
    if (requirements.isNotEmpty()) {
        appendLine("  $label: ${requirements.joinToString { it.render() }}")
    }
}

private fun StringBuilder.appendArtifacts(
    label: String,
    artifacts: List<FeatureDeveloperArtifact>,
) {
    if (artifacts.isNotEmpty()) {
        appendLine(
            "  $label: " + artifacts.joinToString { artifact ->
                "${artifact.id} [${artifact.availability.name.lowercase()}]"
            },
        )
    }
}

private fun FeatureDeveloperCapabilityRequirement.render(): String {
    return when (this) {
        FeatureDeveloperCapabilityRequirement.Always -> "always"
        is FeatureDeveloperCapabilityRequirement.Provided -> capability.id
        is FeatureDeveloperCapabilityRequirement.AllOf -> terms.joinToString(
            prefix = "all-of(",
            postfix = ")",
        ) { it.render() }
        is FeatureDeveloperCapabilityRequirement.AnyOf -> terms.joinToString(
            prefix = "any-of(",
            postfix = ")",
        ) { it.render() }
    }
}
