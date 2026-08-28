---
name: performance-run
description: Plan and execute evidence-backed performance testing for a named feature in the current repository and available target environment. Use when asked to benchmark, profile, measure, or diagnose feature performance. It does not itself authorize implementation or tracked benchmark changes.
---

# Performance Run

Produce a trustworthy performance diagnosis for the requested feature. Measure observable feature behavior, prove that each retained run exercised the intended behavior, preserve the evidence, and limit conclusions to the tested envelope.

## Preserve the authorization boundary

- Treat repository inspection, builds needed for measurement, and non-destructive measurement on an explicitly provided test environment as investigation work.
- Do not modify tracked production code, tests, benchmark harnesses, fixtures, dependencies, or commits unless the user separately authorizes those changes.
- Do not clear app data, replace databases, or materially alter persistent fixtures unless the request explicitly permits that device or data manipulation.
- Keep feature-specific automation and generated evidence in the ignored investigation workspace rather than tracked source.

## Establish the run

1. Read the applicable repository instructions.
2. Identify the feature, concrete user or system behavior, target runtime, dataset or corpus, relevant configurations, and the performance question. If the user supplied no threshold, diagnose distributions and outliers without inventing a pass/fail budget.
3. For a new investigation, use the `create-agent-workspace` skill to create a fresh `<feature>-performance` workspace. Add `PROTOCOL.md` for scenarios and validity gates and an `artifacts/` directory for raw evidence. Resume an existing workspace only when the user identifies or authorizes it; do not read or reuse an unrelated investigation workspace.
4. Trace the feature's runtime ownership and expensive seams before selecting tools. Static analysis may form hypotheses; it is not a performance result.
5. Read [references/katari-android-measurement.md](references/katari-android-measurement.md) before building, installing, or measuring.

## Design the measurement protocol

Choose metrics that match the behavior rather than treating all performance as frame jank:

- frame timing and input responsiveness for scrolling, animation, and transitions;
- end-to-end latency for user actions and state changes;
- startup or loading timing for entry flows;
- throughput, CPU, allocation, garbage collection, or memory for data-heavy and sustained work.

Use more than one metric family when the suspected delay crosses runtime boundaries. Separate primary performance measurements, evidence that verifies the exercised behavior, and supporting resource or log signals.

Define several realistic scenario families appropriate to the feature. Include ordinary use, a meaningful stress case, and state or data boundaries likely to expose different work. Vary interaction patterns when real users can exercise the feature in materially different ways; do not manufacture unreachable states merely to create coverage.

Every retained run needs an observable validity oracle:

1. Pre-run evidence identifies the intended build, process, feature state, and starting data.
2. Pilot evidence proves that the automation performs the intended actions.
3. In-run evidence shows the target behavior or state progressing, with checkpoints where navigation mistakes could go unnoticed.
4. Post-run evidence confirms the expected final state or transition.
5. No unexpected dialog, surface, failed input, or environment change invalidated the measurement.

Record rejected runs and reasons, and exclude them from aggregates. Tool success or elapsed automation time alone is not proof of a valid run.

## Execute and analyze

- Record exact commands, tool and build configuration, environment, initial state, scenario definitions, and warm-up policy before relying on results.
- Keep warm-ups separate from measured samples. Predeclare a repetition approach appropriate to the tool, variance, and cost; never make a recurrence or comparative claim from one measured run.
- Monitor environmental drift that can invalidate comparisons, such as thermal state, refresh rate, compilation mode, background work, network conditions, or changed data.
- Preserve raw outputs whenever practical. Summarize distributions and worst observed events, not only averages. Use percentiles only when the sample count makes them meaningful.
- Re-run and trace important outliers to determine whether they recur at the same feature event. Distinguish measured facts, correlations, and inferred causes.
- If trustworthy measurements cannot be collected, report the investigation as incomplete and explain the blocking evidence instead of substituting a static guess.

## Complete the handoff

Report:

- repository revision, build/package/process, device or host, dataset, configuration, and measurement tools;
- scenarios, warm-ups, valid run counts, rejected run counts, and rejection reasons;
- metric distributions and exact reproducible slow moments or operations;
- correlations and likely owners only where evidence supports them;
- confidence, limitations, and the precise tested envelope.

Do not claim that no lag or regression exists outside that envelope. Stop after diagnosis unless the user separately asks for and authorizes implementation.
