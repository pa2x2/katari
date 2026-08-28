# Katari Android Performance Measurement

Read the live repository configuration before acting; these notes preserve decision-critical seams, not a substitute for current Gradle and repository instructions.

## Resolve the actual target

- `debug` installs `app.katari.dev`. The release-derived, profileable `benchmark` build uses the separate `app.katari.benchmark` application ID. Installing one does not update or populate the other.
- Confirm the target package after installation and verify that its process started or restarted. Do not infer the installable variant from the FOSS unit-test build type.
- Treat app data as package-specific. A corpus or database visible to the development package is not automatically available to the benchmark package. Provision or alter persistent test data only when the request authorizes it.
- Inspect the current `baseline-profile` module before relying on it. Its existing benchmarks may cover startup without providing a harness or validity oracle for the requested feature.

## Record the environment

Capture the device serial, model, OS/API level, refresh rate, resolution and density, power and thermal state, target package/version, build type, compilation or Baseline Profile mode, and feature configuration. Record meaningful changes during the run.

Prefer a physical connected test device for conclusions about user-perceived Android performance. An emulator or managed device can help develop automation or generate profiles, but do not silently substitute it for a requested physical-device conclusion.

## Select evidence deliberately

- Use Macrobenchmark when an existing or authorized harness can drive and validate the scenario.
- Use FrameTimeline, system traces, or another platform frame source for UI smoothness; use UI hierarchy and screenshots to prove the correct surface and state.
- Use input timestamps, logs, CPU or memory snapshots, garbage collection events, and automation timing as supporting signals unless they directly measure the stated performance question.
- Pilot gesture or navigation automation with bounded steps and checkpoints. UI automation can successfully issue input while exercising the wrong screen or skipping the event under test.

Keep compilation mode, profile installation, build variant, and data state comparable across runs. Follow the repository's current Gradle invocation and diagnostic-log requirements; do not suppress build warnings or combine incompatible variant properties.

## Device-state authority

For an explicitly provided test device, launching the app, installing the matching test build, capturing diagnostics, and reversible UI-state setup are ordinary measurement steps. Clearing package data, replacing a database, or materially changing persistent fixtures requires explicit authorization unless the request already grants it. Record state-changing setup and any restoration performed.
