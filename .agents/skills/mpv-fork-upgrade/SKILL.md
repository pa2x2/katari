---
name: mpv-fork-upgrade
description: Bring the pa2x2/mpv-android fork, which builds Katari's libmpv AAR and the mpv add-on, up to date: report what upstream mpv-android and each pinned native dependency (mpv, FFmpeg, dav1d, libass, libplacebo and the rest) offer, merge and re-pin in the fork, build and test the add-on, hand the release over to the user, and then move Katari's libmpv pin when the report says it must. Use when the user asks to upgrade, update or sync the mpv-android fork, the mpv add-on, libmpv, mpv or FFmpeg, and when a dependency audit of Katari is asked to cover everything.
---

# mpv-android fork upgrade

`pa2x2/mpv-android`, branch `katari-lgpl`, is a fork of `mpv-android/mpv-android` that builds two things Katari depends on. Its releases are tagged `libmpv-YYYY-MM-DD-N` and publish both together:

- **The libmpv AAR.** Katari pins one release of it (`libmpv` in `gradle/libs.versions.toml`, hash in `gradle/verification-metadata.xml`) and takes only its compiled-in classes (`MPVLib`) and `assets/libmpv/` from it (`video-engine/mpv/build.gradle.kts`). The AAR's `load-order.json` states the JNI glue level and the minimum add-on the app requires (`MpvAddonRequirement.kt`). Katari's licence screen is generated at build time from the AAR's `third-party.json` (`gradle/build-logic/.../licences/LibmpvNotices.kt`).
- **The mpv add-on APKs** (`app.katari.mpv`, one per ABI: arm64-v8a, armeabi-v7a, x86_64). They hold the native libraries. Users install and update them separately from the app, which finds new ones through `MpvAddonReleaseSource.kt`.

The work has three parts, each stopping for the user:

1. **Report**: what a new fork release would bring and whether Katari's pin must follow.
2. **Fork upgrade**: merge, re-pin, build, test, hand over the release.
3. **Katari pin bump**: only when the report says it must move, and only after the user has published the release.

Upgrading the fork is not the same as merging upstream. Upstream mpv-android pins only some of its dependencies; it clones mpv, FFmpeg (outside CI), dav1d, libass and libplacebo from their master branches, so every fresh upstream build gets their newest code. The fork pins every source by checksum or commit, so those five only move when this skill moves them. A fork upgrade can be worth a release when upstream has no new commits at all.

For risk tiers, the report group template and the verification levels, follow the `dependency-upgrades` skill; this skill adds what is specific to the fork. For merge safety, reuse the rules of `.agents/skills/merge-upstream/SKILL.md` (re-resolve the approved SHA, never stash or discard, real merge, ancestry preserved), not its Mihon and Entry specifics.

## Locate the fork

Use the path the user gave. If they gave none, ask; a sibling `mpv-android` checkout may exist and may not be the one they mean. Check it with `git -C <path> remote -v`: `origin` must be `pa2x2/mpv-android`, `upstream` must be `mpv-android/mpv-android`, and `katari-lgpl` must exist. If the path is missing or doesn't match, stop and ask. Never clone it yourself. Other threads work in the fork too: if it is on another branch or has changes you didn't make, report that and don't touch them.

## Report

Read-only. `git fetch upstream` is the one permitted write. Reading GitHub (`gh release list`, `gh run list`) is fine.

1. **State.** Current branch, whether the worktree is clean, whether local `katari-lgpl` equals `origin/katari-lgpl`, the newest `libmpv-*` release, and the release Katari pins now. If Katari's pin is older than the newest release, say so.

2. **Upstream delta.** Resolve `upstream/master` to a SHA. List `merge-base(katari-lgpl, upstream/master)..upstream/master` and read the diffs, not only the subjects. Also read `merge-base..katari-lgpl`: that is where conflicts come from. Classify each upstream change by what it ships:

   | Touches | Reaches | Needs |
   |---|---|---|
   | Only `app/` (the upstream mpv-android app: UI, settings, MPVView) | Nothing Katari ships | Nothing. |
   | `buildscripts/`, `depinfo.sh` versions, build scripts, the fork's Gradle build | The add-on's native libraries (and the notices in the AAR) | A new release. |
   | `libmpv/` native code (`src/main/jni`) or `MPVLib.kt` / `BaseMPVView.kt` | The glue between the AAR's classes and `libplayer.so` | A new release, and almost always a Katari pin bump. |

   Upstream still edits JNI and `MPVLib.kt` at their old `app/src/main/…` paths, which the fork moved to `libmpv/`. Git follows renames when merging, but check each such change lands in the right file.

3. **Every pin in `buildscripts/include/depinfo.sh`, one by one.** `download-deps.sh` checks each source against `sha_*` (archives) or `commit_*` (git checkouts). The pins fall into three kinds:

   - **Upstream pins a version too**: NDK and SDK, unibreak, harfbuzz, fribidi, freetype, mbedtls, libxml2, fontconfig. Upstream's `v_*` is the target; the merge brings it. Mention a newer release of the project as information, and don't go past upstream for these.
   - **Upstream floats, the fork pins**: mpv, FFmpeg, dav1d, libass, libplacebo. Nothing moves them but this skill. Check each against its own project:
     - **FFmpeg** (`v_ffmpeg`, `sha_ffmpeg`): the newest release in the series upstream's CI builds (`v_ci_ffmpeg` on `upstream/master`). A newer series only if the chosen mpv supports it. When upstream changes `v_ci_ffmpeg`, the merge conflicts with the fork's `v_ci_ffmpeg=n$v_ffmpeg`: keep the fork's line and move `v_ffmpeg` and `sha_ffmpeg` instead. Taking the fork's side without that silently drops upstream's FFmpeg bump.
     - **mpv** (`v_mpv`, `sha_mpv`, `v_mpv_base`, `v_mpv_date`): the newest mpv release if one builds against the FFmpeg in use. Otherwise a master commit, chosen as the one upstream mpv-android's newest successful `build` run on master used (`gh run list -R mpv-android/mpv-android -b master -s success`, and mpv master at that run's time), since that combination is known to build. Say whether the pin can return to a release. Keep `v_mpv_base`, `v_mpv_date` and the comment above them in step with the commit: they go into the notices (`katari-release.sh`).
     - **dav1d** (`v_dav1d`), **libass** (`v_libass`): their newest releases.
     - **libplacebo** (`v_libplacebo`, `commit_libplacebo`): the newest release tag and the commit it resolves to. Check that the submodule list passed to `fetch_git` still matches what the release needs.
   - **Not built**: lua and curl. Their `v_*` and `sha_*` lines are inert (`dep_mpv` leaves them out and nothing fetches them). Upstream changes that add them back, or that rely on them in `mpv.sh`, must be adapted instead of accepted.

   For every pin that would move, read the release notes for every version between the current and the target one, and match them against what Katari uses: the options, properties, commands and events `video-engine/mpv` sets or reads, decoders and formats Katari's sources serve (HLS, DASH, subtitles), and the fork's own patches (`buildscripts/patches/<dep>/*.patch`), which must still apply. Drop a patch when the target version contains an equivalent fix, and say so. Give `file:line` for every match, as the `dependency-upgrades` skill asks.

   Compute each new checksum from the real archive. Where the project publishes a checksum or signature, compare against it, and say which ones don't.

4. **The fork's Gradle build.** The Gradle wrapper, AGP, Kotlin (`build.gradle`) and compileSdk and minSdk (`libmpv/build.gradle`, `katari-addon/build.gradle`) usually arrive with the merge. Two couplings with Katari:
   - The fork's Kotlin must not be newer than Katari's (`kotlin-gradle` in `gradle/libs.versions.toml`): Katari compiles against the AAR's `MPVLib` classes and can't read metadata from a much newer compiler.
   - `libmpv`'s minSdk must not be above Katari's minSdk.
   The NDK isn't coupled: the native libraries ship in the add-on APK with their own libc++.

5. **Release needed.** Yes when any pin moves or any change from the second or third row of the table lands. Otherwise "nothing to publish".

6. **Katari pin must move.** Only when one of these holds:
   - the glue level has to be raised (`MPVLib.GLUE_LEVEL`, `libmpv/glue/level-<N>.txt`; `buildscripts/katari-glue.py check` is the authority, and the release build fails without the bump). The app and the add-on must have the same level, so a new level needs a new app and a matching add-on together;
   - Katari code needs something new from `MPVLib` or the assets (a new event, property, constant, a fixed bug in the compiled-in Kotlin);
   - the app should now require the newer add-on (a security or crash fix in the native libraries): the minimum comes from the pinned AAR's `load-order.json`;
   - the licence screen would describe the wrong components: compare the new release's `third-party.json` with the pinned one. If components or licence texts changed, the pin must move. If only versions changed, say so and let the user decide.

   Otherwise: "publish the add-on release, leave Katari's pin as is", with the reason for each of the four.

7. **Verification plan.** Per the `dependency-upgrades` skill. For an add-on-only release, the key regression check is the one the build can't give: the add-on built from the new release works with the app Katari ships today.

Write the fork as one report group in the `dependency-upgrades` template, with one row per moved pin and per upstream change group, plus two fields: "Release needed" and "Katari pin must move", each with its reason. If the pin must move, add a second group, "Katari libmpv pin", marked "blocked until the fork release is published". When this report is part of a full Katari dependency audit, return these groups unchanged for it to place in its order.

Stop after the report.

## Fork upgrade

Only after the user approved the report.

1. **Worktree.** The fork must be clean on `katari-lgpl`. Re-fetch and confirm `upstream/master` still resolves to the approved SHA.
2. **Branch.** Create `upstream-sync/<YYYY-MM-DD>` from `katari-lgpl`. Never merge into `katari-lgpl` directly and never rebase: the user merges.
3. **Merge.** If upstream has new commits, `git merge --no-ff --no-commit <approved-sha>`. Resolve conflicts keeping the fork's pins and layout and taking upstream's intent. Read the files git merged cleanly too, especially `depinfo.sh`, `download-deps.sh` and `mpv.sh`.
4. **Re-pin.** Set every approved pin from the report and recompute its `sha_*` or `commit_*`, including the versions upstream moved. Update and re-check the patches.
5. **Commit.** `katari-release.sh` refuses a working tree with uncommitted changes, because the source bundle must match the build, so the merge and pins must be committed on the sync branch before building. If the user hasn't authorized commits, stop here and ask.
6. **Fresh sources.** `download-deps.sh` skips any `deps/<dir>` that already exists, without checking it. Remove `buildscripts/deps/<dir>` for every pin that moved (and the patched ones), or the build silently uses the old source.
7. **Glue.** The release build runs `katari-glue.py check`. If the glue changed, raise `MPVLib.GLUE_LEVEL` and run `katari-glue.py write` (never edit an existing level file), and commit that. The Katari pin now must move.
8. **Build.** `buildscripts/katari-release.sh --arch arm64`, plus `--arch x86_64` when an emulator will test it. It builds the AAR, the add-on APKs, the notices and the load order, checks that FFmpeg and mpv are LGPL, and runs the glue check. It needs the NDK, bash 4, python3 and a JDK, and takes long: tell the user, write a log file, and look for `error`, `warning` and `FAILED` in it. Without `--release` the APKs are signed with the debug key, so a locally built add-on can't update over a release-signed one: say so before installing anything.
9. **Test the add-on against the app.** Install the arm64 add-on on the test device and play video and audio from several kinds of source, including HLS and DASH with subtitles, with the Katari build users have now (AGENTS.md says which variant to install and how to launch it). Seek, switch tracks, background and resume. Check the add-on's version and glue level as the app reports them. When the glue changed, check that the old app reports an add-on mismatch cleanly instead of crashing. Watch logcat for crashes and linkage errors throughout.
10. **Hand over.** Pushing, tagging and creating releases are the user's job. Give the exact commands: merge `upstream-sync/<date>` into `katari-lgpl` with `--no-ff`, push, then tag. Name the tag: `libmpv-<today>-<N>`, N one more than the highest N released today, starting at 1, at most 99, and never a date earlier than the last release (the add-on's versionCode is built from it). Say what the `katari-release` run produces and what to check: the four secrets `SIGNING_KEY`, `KEY_STORE_PASSWORD`, `ALIAS` and `KEY_PASSWORD` exist, and the APKs are signed with Katari's key.

Report as the `dependency-upgrades` skill describes, including what was left on the machine (the sync branch, `buildscripts/release/`, the add-on installed on the device).

## Katari pin bump

Only when the report said the pin must move, and only after the user says the release is published. Confirm it with `gh release view` (read-only), including the AAR and its `SHA256SUMS` file. Work in Katari on a branch of its own, as AGENTS.md and the user's branching rules say.

1. Change `libmpv` in `gradle/libs.versions.toml`, and its comment if the glue level changed.
2. Replace the libmpv component in `gradle/verification-metadata.xml` with the new version, artifact name and SHA-256 from the release's `SHA256SUMS`. Download the AAR and hash it yourself to check the published value.
3. Fix what the new `MPVLib` or assets need in `video-engine/mpv`, `MpvAddonRequirement.kt` and `LibmpvNotices.kt`. The report's reason names the code.
4. Resolve once without caches (`--refresh-dependencies`, or a fresh Gradle user home): the AAR comes from a GitHub release URL, and a build that passes only from the cache will fail on the next clean machine.
5. Run the validation from AGENTS.md: `spotlessApply`, `verifyTests`, then the telemetry release compilation in a separate invocation.
6. On the test device, installed as an update over existing data:
   - the new app with the new add-on;
   - the new app with the previous add-on: when the minimum rose, the app must offer the update, not fail;
   - the previous app with the new add-on, if the glue is unchanged;
   - the add-on download and update flow from the app against the real release.
7. Report as for any group.
