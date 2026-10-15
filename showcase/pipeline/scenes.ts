/**
 * Runs the UiAutomator classes of showcase/capture against the app. Each scene leaves its stills and clips in
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
 * In the order they run. Opening a reader records a reading session at the showcase moment, so every scene that shows
 * reading history comes before the first one that opens a reader.
 */
export const SCENES = [
  "library.LibraryScenes",
  "library.EntryScenes",
  "statistics.StatisticsScenes",
  "statistics.RecapScenes",
  "browse.BrowseScenes",
  "translation.TranslatorScenes",
  "translation.TranslationSettingsScenes",
  "settings.BackupScenes",
  "translation.TranslationScenes",
  "reader.BookScenes",
  "reader.PlayerScenes",
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
