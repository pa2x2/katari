/**
 * Runs the UiAutomator classes of tools/showcase/capture against the app. Each scene leaves its stills and clips in
 * CAPTURE_DIRECTORY on the device, and a screenshot of whatever was on screen when it failed, with the app's and the
 * system's state at that moment, in FAILURE_DIRECTORY.
 */
import type { Device } from "./device/adb.ts";

export const CAPTURE_PACKAGE = "app.katari.showcase.capture";
export const CAPTURE_DIRECTORY = "/data/local/tmp/showcase";
export const FAILURE_DIRECTORY = "/data/local/tmp/showcase-failures";
// Several times the slowest scene on a CI runner, which takes about three minutes.
const SCENE_TIMEOUT_MS = 15 * 60_000;

/**
 * The scene that records the recap clip, which the pipeline re-encodes for the site with ffmpeg, and leaves the image
 * the recap shares in the app's cache.
 */
export const RECAP_SCENE = "statistics.RecapScenes";

/**
 * The scenes, in groups that each run on their own freshly set up emulator, so CI runs the groups side by side. No scene
 * may depend on what a scene of another group did. Within a group they run in this order: opening a reader records a
 * reading session at the showcase moment, so every scene that shows reading history comes before the first one of its
 * group that opens a reader. The groups are balanced by how long they take on a CI runner, where the recap alone takes
 * about five minutes.
 */
export const SCENE_GROUPS: Record<string, string[]> = {
  recap: [RECAP_SCENE],
  translation: [
    "translation.TranslatorScenes",
    "translation.TranslationSettingsScenes",
    "translation.TranslationScenes",
  ],
  "library-and-statistics": ["library.LibraryScenes", "library.EntryScenes", "statistics.StatisticsScenes"],
  "browse-and-backup": ["browse.BrowseScenes", "settings.BackupScenes"],
  readers: ["reader.BookScenes", "reader.ComicScenes", "reader.PlayerScenes"],
};

export const SCENES = Object.values(SCENE_GROUPS).flat();

/** The scenes that translate with Offline Translator, which setup then installs and readies on-page translation for. */
export const TRANSLATION_SCENES = SCENE_GROUPS.translation!;

/**
 * The scenes that find the Personal profile's year recap opened. Until it is, More carries a mark for it, which the
 * library's stills show beside the recap the statistics dashboard offers, and the rest of the showcase leaves out.
 */
export const YEAR_RECAP_OPENED_SCENES = [
  ...SCENE_GROUPS.translation!,
  ...SCENE_GROUPS["browse-and-backup"]!,
  ...SCENE_GROUPS.readers!,
];

export function runScene(device: Device, appId: string, scene: string, args: Record<string, string> = {}): void {
  const extras = Object.entries({ targetAppId: appId, ...args }).flatMap(([name, value]) => ["-e", name, `'${value}'`]);
  const output = device.adbWithin(
    SCENE_TIMEOUT_MS,
    "shell",
    "am",
    "instrument",
    "-w",
    ...extras,
    "-e",
    "class",
    `${CAPTURE_PACKAGE}.${scene}`,
    `${CAPTURE_PACKAGE}/androidx.test.runner.AndroidJUnitRunner`,
  );
  // Instrumentation exits with 0 whether or not its tests pass; only its summary tells.
  if (!/^OK \(\d+ tests?\)$/m.test(output)) throw new Error(`${scene} failed:\n${output}`);
}
