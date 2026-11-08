/**
 * Runs the UiAutomator classes of tools/showcase/capture against the Android app. Each scene leaves its stills and clips
 * in CAPTURE_DIRECTORY on the device, and a screenshot of whatever was on screen when it failed, with the app's and the
 * system's state at that moment, in FAILURE_DIRECTORY.
 */
import type { Device } from "./adb.ts";

export const CAPTURE_PACKAGE = "app.katari.showcase.capture";
export const CAPTURE_DIRECTORY = "/data/local/tmp/showcase";
export const FAILURE_DIRECTORY = "/data/local/tmp/showcase-failures";
/** Where a scene with another alongside it signals that it is ready for that one (ShowcaseDevice.signalReady). */
export const READY_SIGNAL = "/data/local/tmp/showcase-ready";
// Several times the slowest scene on a CI runner, which takes about three minutes.
const SCENE_TIMEOUT_MS = 15 * 60_000;

export async function runInstrumentation(
  device: Device,
  appId: string,
  scene: string,
  args: Record<string, string> = {},
): Promise<void> {
  const extras = Object.entries({ targetAppId: appId, ...args }).flatMap(([name, value]) => ["-e", name, `'${value}'`]);
  const output = await device.adbAsync(
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
