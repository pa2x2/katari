---
name: katari-dependency-upgrades
description: Audit Katari's dependencies, and the pa2x2/mpv-android fork that supplies its libmpv AAR and mpv add-on, and write a report of available upgrades grouped by risk, with the gain, the likely failures and a verification plan for each group. Then carry out the groups the user picks, one at a time, with verification strong enough to catch regressions before they ship. Use this whenever the user wants to upgrade, update or bump dependencies, packages, libraries, plugins, SDKs or toolchains, asks what is outdated or can be upgraded, asks whether an update is safe or worth it, wants the mpv-android fork or the mpv add-on brought up to date with upstream, or pastes a list or table of upgrades and asks to perform them "without regressions".
---

# Katari dependency upgrades

The work has two phases.

1. **Report.** Read-only. Find what can be upgraded, what each upgrade gives this project, what can go wrong here specifically, and how to verify it. Group the upgrades by risk and recommend an order. Then stop.
2. **Upgrade.** Only for the groups the user picks. Apply, fix, verify, report.

Grouping by risk matters for two reasons. Each group becomes one commit and one verification pass, so a regression points to one cause. And the user can stop before a group whose risk they don't want to take on right now.

Katari has two kinds of dependency. Ordinary ones (Gradle catalog, toolchains, platform levels, the ONNX Runtime AAR) are handled by Phases 1 and 2 below. The libmpv AAR and the mpv add-on come from a separate repository, the user's fork `pa2x2/mpv-android`, which has its own upgrade flow: see [The mpv-android fork](#the-mpv-android-fork). Include the fork in a report whenever the user asks for everything, for the fork, or for the mpv add-on. Otherwise mention it as available and leave it out.

## Phase 1: report

### 1. Find everything that pins a version

Versions hide in more places than the main manifest:

- Manifests, lockfiles, version catalogs, BOMs and platform constraints.
- Toolchain pins: Gradle wrapper, `packageManager` field, `.nvmrc`, `.tool-versions`, `rust-toolchain.toml`, `requires-python`, `go` directive.
- Platform levels: compileSdk, targetSdk, minSdk, NDK, iOS deployment target, framework SDK (Expo SDK, Flutter SDK).
- Dependencies pinned to a git commit (JitPack, git URLs, forks) and prebuilt binaries checked into the repo, along with the scripts that build them.
- Shared version knobs. A version that another script also reads (an NDK version that an AAR build script uses) can't be bumped on its own.
- AARs served from GitHub releases rather than a registry: ONNX Runtime and libmpv (`settings.gradle.kts` `exclusiveContent`, hashes in `gradle/verification-metadata.xml`). Ask the release list, not Maven. For libmpv the release is made from the fork, so its candidates come from the fork section below.

Find out what runtime targets exist for testing: connected devices, emulators or simulators (`adb devices`, `emulator -list-avds`, `xcrun simctl list`), browsers, a local server. Some checks in the plan will need them.

### 2. Find what is available

Ask the registries. Don't rely on memory.

Sort every candidate into one of these:

- Newer stable release.
- Prerelease only (alpha, beta, RC, preview).
- Blocked by a constraint: a framework SDK that pins a set of versions, a peer dependency range, a plugin's supported tool versions. Name the constraint.
- Commit-pinned. Compare against the upstream head, and check that the pinned artifact is still served (see failure mode "Artifact only resolves from cache").

If the project deliberately uses an alpha line (say a Compose alpha BOM), compare within that line. The newest "stable" may be a downgrade.

For BOMs and platform bundles, find out what a bump actually moves. A BOM bump that only moves one artifact is a one-artifact upgrade.

### 3. Read what changed and match it to this codebase

For each candidate, read the release notes for every version between current and target, not only the target. Then search the codebase for each changed API, behavior, default, keep rule or config option.

This is what makes the report worth reading. "May contain breaking changes" tells the user nothing. This does: "`Slider` now handles `onValueChange` itself instead of passing it to `SliderState`. `presentation-core/.../Slider.kt:37-50` creates a `SliderState` and also passes `onValueChange`, which is exactly the pattern that changed. Dragging could jump or stop updating."

Give `file:line` references for every match. Mark guesses as guesses. If you couldn't read a changelog (rate limit, no changelog, native code), say so and rate the risk higher.

### 4. State the gain

For each upgrade, say what this project gets from it: fixes for bugs in code paths it uses, security fixes, performance, a feature it needs, or unblocking another upgrade. "Nothing" is a valid answer. A new Firebase BoM that changes none of the artifacts the project uses has no gain.

An upgrade with no gain and a real cost (new warnings you can't fix, a migration, a native rebuild) goes under "held back", with the condition that would change that ("revisit with the next ONNX Runtime rebuild").

### 5. Rate the risk

Semver doesn't measure risk. Rate each upgrade on three things:

- **Likelihood.** Do the changes touch APIs, behavior or config that this code uses?
- **Blast radius.** What breaks if it goes wrong? The build, one screen, background work, stored user data, authentication, every launch?
- **Detectability.** Where would the failure show up? At compile time (the good outcome), in tests, only at runtime, only in a minified release build, only when upgrading over existing data, or silently (a connection that hangs, a job that never runs)? Low detectability raises risk more than anything else.

Then place it in a tier:

| Tier | Typical content | Commit |
|---|---|---|
| Very low | Patch or small minor. Changelog read, nothing changed that this code uses, any failure would show at build time. | Several can share one commit. |
| Low | Small behavior changes in areas used here, native code whose changelog you couldn't fully read, tooling with cache or config changes. | One commit per group, a quick runtime check. |
| Medium | Generated code changes, storage or migrations, UI component behavior on call sites you found, binary compatibility between separately pinned artifacts, lifecycle or timing changes, security-sensitive flows. | One commit per dependency. |
| High | Platform target changes (targetSdk, runtime or framework majors), anything that needs new permissions, user-facing decisions or a migration plan. | Its own project with its own plan. Not a bump. |
| Held back | Prerelease only, blocked by a constraint, or no gain. | None. Say what unblocks it. |

### 6. Order the groups

- Build tooling first, so library changes get tested on the toolchain you'll ship. Exception: risky tooling with little gain can wait until the end, or be deferred.
- Cheap, safe groups before risky ones.
- Upgrades that must move together go together: framework SDK sets, BOM members, a library that needs a newer compileSdk. Note such couplings explicitly, since they can force a group to grow.
- Anything that could cause a regression on its own gets its own commit.

### 7. Write the report

Use this structure. Each group has to stand on its own, because the user may paste one group into a new session. Repeat the file locations, versions and checks in every group rather than writing "as above".

```markdown
# Dependency upgrade plan: <project>

## Recommended order

| # | Group | Risk | Main check |
|---|---|---|---|

## Group <n>: <name> (<tier> risk)

Declared in: <files>

| Dependency | Current → target | Gain for this project | What could go wrong here |
|---|---|---|---|

<For medium and high risk items, a short section per dependency: the specific changes, the matching code with file:line, and why the failure would or wouldn't be caught at build time.>

Verification:
- Gate: <the exact commands from the repo rules and CI>
- Build: <variants to build, including the release/minified one when relevant>
- Runtime: <each affected flow as start state → action → expected end state>
- Data: <upgrade over existing data, migration from the last released version, if relevant>
- Needs: <devices, emulators, accounts or data you don't have>

## Held back
- <dependency> <current → available>: <reason>. Revisit when <condition>.

## Not verified
<Changelogs you couldn't read, guesses you couldn't confirm.>
```

## Phase 2: upgrade

Work through the groups the user authorized, in order. Keep each group's changes in its own commit and keep unrelated fixes out of it. Commit only if the user has authorized commits for this work; otherwise leave the changes uncommitted and say so.

### Before changing anything

1. Check the worktree. If it has changes you didn't make, leave them out of everything you stage.
2. Run the project's verification gate once on the unchanged tree and note the warnings and test counts. Later, a warning only counts as "caused by the upgrade" if it wasn't there before. When you can't tell, build the base commit in a separate git worktree and compare.
3. For dependencies that generate code (SQL, protobuf, GraphQL, ORMs, serializers), save the generated output so you can diff it after the upgrade.
4. If a library reads or writes a format the project persists (editor markdown, serialized settings, a database), write a characterization test that passes on the current version first. If it still passes after the upgrade, the format survived. If the project has no test for that contract, this test is worth keeping.

### Apply

- Use exactly the versions in the plan or the user's message. If something newer exists, mention it. Don't quietly go further.
- Prefer the ecosystem's own tool (`./gradlew wrapper`, `npx expo install --fix`, `cargo update -p`). Then read the whole diff. Generators reset custom settings; the Gradle wrapper task, for example, dropped a deliberate `retries=3`. Restore such settings in a way that survives the next regeneration.
- Inspect the resolved dependency graph afterwards. Every target version should resolve as intended, nothing else should have moved unexpectedly, and nothing the code imports directly should have disappeared from the graph.

### Fix what breaks

- Fix the cause. Don't suppress warnings, disable lint rules, add baseline entries, add catch-all branches or pin around a problem to get a green build. If a stricter lint rule or a new compiler check flags something real, improve the code. The user has said explicitly: no cheap fixes.
- If the code used something it only got transitively, declare it explicitly. If that thing is deprecated, say so; replacing it is separate work.
- If the proper fix is a large redesign unrelated to the upgrade, stop and ask. Offer the choices: do it now, a narrow interim fix, or defer the upgrade.
- If an upgrade turns out to give nothing and costs something (an upstream deprecation warning on every build that you can't fix locally), revert it and report it as deferred, with what would change that.

### Verify

Run every level the group's plan calls for, and extend the plan when the changes you made touch more than expected. Report evidence for each level, not just "passed".

1. **Static.** Formatting, lint, typecheck and compilation of every variant, flavor and target the project ships.
2. **Tests.** The full suite CI runs, plus focused tests for code you touched.
3. **Dependency audit.** Resolved versions match. For git-pinned or unusually hosted artifacts, resolve once without caches; a build that only works from cache will fail on the next clean machine.
4. **Production-equivalent build.** Minified, optimized, tree-shaken or AOT. Many upgrade regressions only exist there.
5. **Runtime, in place.** Install as an update over existing data, not a fresh install, and confirm the new build is the one running (new process, new version). This is where migrations, cached state and stored formats break.
6. **Affected features, end to end.** Exercise each affected feature through complete state transitions, not "the screen opens". Enable a setting, let it persist, cold-restart, re-enter the flow, complete it, then restore the setting. Repeat cycles where timing matters. Include what lives outside the compile graph: plugins or extensions loaded at runtime, background and scheduled jobs, widgets, deep links. Watch the logs for crashes and linkage errors during all of it.
7. **Release runtime.** Install the production-like build and repeat the critical paths. In one past upgrade this step alone found a class that the minifier removed but a runtime-loaded extension still needed.

When a check can't run (no device with the right OS version, no test account), say which risk stays unverified instead of implying coverage.

### Report each group

- Commit and changed files, or "uncommitted".
- Code changes the upgrade needed, and why.
- Verification: what you actually exercised and what you observed.
- Problems found that the upgrade didn't cause (confirmed against the base commit). Report them separately and ask before fixing.
- What you deferred, and why.
- What you couldn't verify.
- What you left on the machine or device.

Then go on to the next authorized group, or stop and wait.

## The mpv-android fork

`pa2x2/mpv-android`, branch `katari-lgpl`, is a fork of `mpv-android/mpv-android` that builds two things Katari depends on. Its releases are tagged `libmpv-YYYY-MM-DD-N` and publish both together:

- **The libmpv AAR.** Katari pins one release of it (`libmpv` in `gradle/libs.versions.toml`, hash in `gradle/verification-metadata.xml`) and takes only its compiled-in classes (`MPVLib`) and `assets/libmpv/` from it (`video-engine/mpv/build.gradle.kts`). The AAR's `load-order.json` states the minimum add-on and the JNI glue level the app requires (`MpvAddonRequirement.kt`).
- **The mpv add-on APKs** (`app.katari.mpv`, one per ABI). They hold the native libraries. Users install and update them separately from the app, and the app finds new ones through `MpvAddonReleaseSource.kt`.

"Upgrading the fork" means merging upstream into `katari-lgpl` and publishing a new release, so that the add-on gets upstream's mpv, FFmpeg and library updates. It does not mean bumping the `libmpv` version in Katari. The two are separate steps and often only the first is needed.

### Locate the fork

Don't assume where it is. Use the path the user gave. If they gave none, ask. A sibling `mpv-android` checkout may or may not exist, and it may not be the one they mean. Check the checkout with `git -C <path> remote -v`: `origin` must be `pa2x2/mpv-android`, `upstream` must be `mpv-android/mpv-android`, and `katari-lgpl` must exist. If the path is missing or doesn't match, stop and ask. Never clone it yourself.

### Report: what upstream brings and what it means for Katari

All of this is read-only. `git fetch upstream` is the one permitted write.

1. **State.** Current branch, whether the worktree is clean, whether local `katari-lgpl` equals `origin/katari-lgpl`, the newest `libmpv-*` tag or release (ask `gh release list` or `git tag`), and the version Katari pins now. If Katari's pin is older than the newest release, say so.
2. **Delta.** Resolve `upstream/master` to a SHA. List `merge-base(katari-lgpl, upstream/master)..upstream/master` and read the diffs, not only the subjects. Also read what the fork changed (`merge-base..katari-lgpl`): that is where conflicts come from.
3. **Classify each upstream change by what it ships:**

   | Touches | Reaches | Needs |
   |---|---|---|
   | Only `app/` (the upstream mpv-android app: UI, settings, MPVView) | Nothing Katari ships | No release. Say "nothing to publish" and stop for this commit. |
   | `buildscripts/` versions and scripts, mpv/FFmpeg/library pins, patches | The add-on's native libraries (and notices in the AAR) | A new release. Whether Katari's pin must move is decided in step 4. |
   | `libmpv/` native code (`src/main/jni`) or `MPVLib.kt` / `BaseMPVView.kt` | The glue between the AAR's classes and `libplayer.so` | A new release, and almost always a Katari pin bump too (step 4). |

   Upstream still edits JNI and `MPVLib.kt` at their old `app/src/main/…` paths, which the fork moved to `libmpv/`. Git follows renames when merging, but check each such change landed in the right file.
4. **Decide whether Katari's pin must move.** An add-on can be released on its own as long as the glue stays compatible, so a Katari change is required only when one of these holds:
   - the glue level has to be raised (`MPVLib.GLUE_LEVEL`, `libmpv/glue/level-<N>.txt`; `buildscripts/katari-glue.py check` is the authority, and the release build fails without the bump). The app and the add-on must have the same level, so a new level needs a new app and a matching add-on together;
   - Katari code needs something new from `MPVLib` or the assets (a new event, property, constant, fixed bug in the compiled-in Kotlin);
   - the app should now *require* the newer add-on (a security or crash fix in the native libraries): the minimum comes from the pinned AAR's `load-order.json`;
   - the notices Katari shows (`third-party.json`, licences in the AAR) would otherwise describe libraries that the shipped add-on no longer contains. Check what `LibmpvNotices.kt` and the licences screen actually read before claiming this; if the add-on carries its own notices, this reason doesn't apply.

   Otherwise say: "publish the add-on release, leave Katari's pin as is", and give the reason for each of the four.
5. **Fork-specific hotspots.** Read these even when git merges them cleanly:
   - `buildscripts/include/depinfo.sh` and `download-deps.sh`: the fork pins every source by SHA-256 or commit (`sha_*`, `commit_*`). A version bump from upstream without a new checksum fails the build at best. Compute the new checksum from the real archive and compare it with a checksum the project publishes where there is one.
   - The mpv pin. mpv is held at a master commit because no mpv release builds against FFmpeg 9. Check whether upstream moved it, whether a release now builds against the FFmpeg version in use, and say if the pin can return to a release.
   - lua and curl are deliberately not built (`dep_mpv`). Upstream changes that add them back, or that rely on them in `mpv.sh`, must be adapted instead of accepted.
   - Differences between the fork's pins and the versions upstream's CI currently builds. Report them as information; don't bump past upstream (see Phase 2 rules).
6. **Verification plan.** State it per group (see Phase 2). For an add-on-only release the key regression check is the one the build can't give: **the add-on built from the new release works with the app Katari ships today**.

Put the fork in the report as its own group (usually first, since a Katari pin bump depends on it). Add two fields to the group: "Release needed" and "Katari pin must move", each with the reason from steps 3 and 4. If a Katari pin bump is needed, write it as a separate following group that says "blocked until the fork release is published".

### Upgrade: the fork

Plan first, as `merge-upstream` does: stop after the report and carry on only when the user has approved it. Reuse its safety rules (see `.agents/skills/merge-upstream/SKILL.md`: re-resolve the approved SHA, don't stash or discard anything, real merge, ancestry preserved), but not its Mihon and Entry specifics, which don't apply to this repository.

1. **Fork worktree.** Must be clean on `katari-lgpl`. Re-fetch and confirm `upstream/master` still resolves to the approved SHA.
2. **Branch.** Create `upstream-sync/<YYYY-MM-DD>` from `katari-lgpl` in the fork. Never merge into `katari-lgpl` directly and never rebase: the user merges.
3. **Merge.** `git merge --no-ff --no-commit <approved-sha>`. Resolve conflicts, keeping the fork's pins and layout and taking upstream's intent. Recompute every `sha_*` / `commit_*` the merge changed, as described above.
4. **Glue.** Run `buildscripts/katari-glue.py check` against a built AAR. If the glue changed, raise `MPVLib.GLUE_LEVEL` and run `write` (never edit an existing level file). Note that this changes the "Katari pin must move" answer to yes.
5. **Build.** `buildscripts/katari-release.sh --arch arm64` first (add `--arch x86_64` when an emulator will test it). It builds the AAR, the add-on APKs, the notices and the load order, and runs the glue check. It needs the NDK, bash 4, python3 and a JDK, and takes long: tell the user, run it with a log file, and look for `error`, `warning` and `FAILED` in the log. Without `--release` the APKs are signed with the debug key, so a locally built add-on can't update over a release-signed one. Say so before installing anything.
6. **Test the add-on against the app.** Install the locally built arm64 add-on on a test device and play video and audio from several kinds of source with the Katari build that users have now (see AGENTS.md for which variant to install and how to launch it). Also check the add-on's reported version and glue level in the app. When the glue changed, also check that the *old* app reports an add-on mismatch cleanly instead of crashing.
7. **Commit** the merge on the sync branch only if the user has authorized commits. Leave the sync branch for the user to merge.
8. **Hand over, don't publish.** Publishing a release is the user's job: pushing branches or tags, and creating releases, are GitHub changes the agent doesn't make. Stop and give them the exact steps, as copyable commands: merge `upstream-sync/<date>` into `katari-lgpl` with `--no-ff`, push, then tag. Work out the next tag from the existing release list (`libmpv-<today>-<N>`, N being one more than the highest N already released today, starting at 1) and name it. Say what the CI run will produce and what to check (signed with Katari's key, the four release secrets exist).

### Upgrade: Katari's pin (only when the report said it must move)

Do nothing here until the user says the release is published. Then confirm it from the release page or `gh release view` (read-only), including the AAR and the `SHA256SUMS` file.

1. Change `libmpv` in `gradle/libs.versions.toml` (and the comment there if the glue level changed).
2. Update `gradle/verification-metadata.xml` with the new component, artifact name and SHA-256 from the release's `SHA256SUMS`. Download the AAR and hash it yourself to check the published value. Remove the old component entry.
3. Fix what the new `MPVLib` / assets need in `video-engine/mpv`, `MpvAddonRequirement.kt`, and wherever the notices are read. The reason in the report names the code.
4. Resolve once without caches (`--refresh-dependencies`, or a fresh Gradle user home): the artifact comes from a GitHub release URL, and a build that passes only from the cache will fail on the next clean machine.
5. Run the repository's validation from AGENTS.md (`spotlessApply`, `verifyTests`, then the telemetry release compilation in a separate invocation). Do not combine them.
6. Runtime on the test device, as an update over existing data: new app + new add-on (the matching pair), new app + the previous add-on (when the minimum allows it, the app must offer the update, not fail), previous app + new add-on if the glue is unchanged. Then follow the add-on download and update flow from the app against the real release.
7. Report as for any group.
