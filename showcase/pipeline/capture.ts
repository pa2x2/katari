/**
 * Captures every showcase still and clip on a freshly wiped emulator, the same way locally and in CI.
 *
 * Setup runs at the real time, because the text recognition models download over HTTPS: it restores the seed backup
 * (seed/generate.ts) into the benchmark build and readies on-page translation. Then the clock moves to the showcase
 * moment and the scenes run. Expects the content server's folder (content/fetch.ts), the seed, and the three APKs built:
 *
 *   ./gradlew :app:assembleBenchmark :showcase:extension:assembleDebug :showcase:capture:assembleBenchmark
 */
import { type ChildProcess, spawn } from "node:child_process";
import { existsSync, mkdirSync, rmSync, writeFileSync } from "node:fs";
import { join, resolve } from "node:path";
import { parseArgs } from "node:util";
import { setTimeout as sleep } from "node:timers/promises";
import { EMULATOR_CONTENT_SERVER } from "../content/content-server.ts";
import { cacheDir } from "../content/download-cache.ts";
import { profiles } from "../seed/library-plan.ts";
import { LIBRARY_UPDATED_AT } from "../seed/showcase-clock.ts";
import { writeCredits } from "./credits.ts";
import { attachedSerials, Device } from "./device/adb.ts";
import { awaitIdle } from "./device/device-idle.ts";
import { pinShowcaseClock, useRealClock } from "./device/device-clock.ts";
import { bootShowcaseEmulator, SHOWCASE_SERIAL, shutDown } from "./device/emulator.ts";
import { installOfflineTranslator, startOfflineTranslator } from "./device/offline-translator.ts";
import { writePreferences } from "./device/shared-preferences.ts";
import { freezeStatusBar } from "./device/status-bar.ts";
import { CAPTURE_DIRECTORY, FAILURE_DIRECTORY, runScene, SCENES } from "./scenes.ts";
import { encodeForWeb } from "./web-clip.ts";

const APP_ID = "app.katari.benchmark";
const APP_PREFERENCES = `${APP_ID}_preferences.xml`;
const RECAP_SHARE_IMAGE = `/data/data/${APP_ID}/cache/shared_image/statistics_recap.png`;
const SEED_NAME = "showcase.tachibk";
const SEED_ON_DEVICE = `/sdcard/Download/${SEED_NAME}`;

const repository = resolve(import.meta.dirname, "..", "..");
const { values } = parseArgs({
  options: {
    out: { type: "string", default: join(cacheDir(), "captures") },
    seed: { type: "string", default: join(cacheDir(), "showcase.tachibk") },
    served: { type: "string", default: join(cacheDir(), "served") },
    "app-apk": { type: "string", default: join(repository, "app/build/outputs/apk/benchmark/app-x86_64-benchmark.apk") },
    "extension-apk": {
      type: "string",
      default: join(repository, "showcase/extension/build/outputs/apk/debug/extension-debug.apk"),
    },
    "capture-apk": {
      type: "string",
      default: join(repository, "showcase/capture/build/outputs/apk/benchmark/capture-benchmark.apk"),
    },
    "keep-emulator": { type: "boolean", default: false },
    // For working on scenes: runs these, by the class names in scenes.ts and in that order, instead of all of them.
    scenes: { type: "string" },
    // For working on scenes: uses the emulator a run with --keep-emulator left, already set up, and keeps it again.
    // Reader scenes it ran have added reading history, so history-dependent stills drift from a fresh run's.
    "reuse-emulator": { type: "boolean", default: false },
  },
});
const out = resolve(values.out);
const scenes = values.scenes === undefined ? SCENES : SCENES.filter((scene) => values.scenes!.split(",").includes(scene));
if (scenes.length === 0) throw new Error(`No scene in scenes.ts matches ${values.scenes}`);
const keepEmulator = values["keep-emulator"] || values["reuse-emulator"];
for (const input of [values.seed, values.served, values["app-apk"], values["extension-apk"], values["capture-apk"]]) {
  if (!existsSync(input)) throw new Error(`${input} is missing; see the top of pipeline/capture.ts for how to build it.`);
}

rmSync(out, { recursive: true, force: true });
mkdirSync(out, { recursive: true });
const device = values["reuse-emulator"] ? reusedEmulator() : await timed("booting the emulator", bootShowcaseEmulator);
let server: ChildProcess | undefined;
try {
  server = await startContentServer(values.served);
  if (!values["reuse-emulator"]) await prepare(device);
  device.shell(`rm -rf ${CAPTURE_DIRECTORY} ${FAILURE_DIRECTORY}`);
  await timed("waiting for the device to settle", () => awaitIdle(device));
  for (const scene of scenes) {
    // Times shown relative to now, such as "opened 27 minutes ago", would otherwise depend on how long the scenes
    // before took, and cross a minute in some runs but not in others.
    pinShowcaseClock(device);
    await timed(`capturing ${scene}`, () => runScene(device, APP_ID, scene));
  }
  device.pull(`${CAPTURE_DIRECTORY}/.`, out);
  if (scenes.includes("statistics.RecapScenes")) {
    device.pull(RECAP_SHARE_IMAGE, join(out, "recap-share.png"));
    encodeForWeb(join(out, "recap.mp4"));
  }
  writeCredits(join(values.served, "catalog.json"), join(out, "credits.json"));
  console.log(out);
} catch (error) {
  saveDiagnostics(device);
  throw error;
} finally {
  server?.kill();
  // A failure to shut down is reported without replacing the error that ended the run.
  if (!keepEmulator) {
    await shutDown(device).catch((error) => console.error(`the emulator did not shut down: ${error}`));
  }
}

/** Keeps what explains a failed run beside its captures. The device may be gone by now, so each part is optional. */
function saveDiagnostics(device: Device): void {
  try {
    writeFileSync(join(out, "logcat.txt"), device.adb("logcat", "-d"));
  } catch (error) {
    console.error(`the device log could not be saved: ${error}`);
  }
  try {
    device.pull(`${FAILURE_DIRECTORY}/.`, out);
  } catch {
    // The failure happened before any scene could leave a screenshot.
  }
  console.error(`the device log and any screenshots of the failure are in ${out}`);
}

async function prepare(device: Device): Promise<void> {
  useRealClock(device);
  await timed("installing the apps", async () => {
    device.adb("install", "-r", values["app-apk"]);
    // The seed restore scene waits for the notification the app posts once the restore is done.
    device.shell(`pm grant ${APP_ID} android.permission.POST_NOTIFICATIONS`);
    // Without it, the extension list opens with a banner asking for it.
    device.shell(`appops set ${APP_ID} REQUEST_INSTALL_PACKAGES allow`);
    device.adb("install", "-r", values["extension-apk"]);
    device.adb("install", "-r", "-t", values["capture-apk"]);
    await installOfflineTranslator(device);
    startOfflineTranslator(device);
  });

  // The apps just installed keep the system busy for minutes, which slows the screens the seed restore taps through.
  await timed("waiting for the device to settle", () => awaitIdle(device));
  const seedUri = await pushSeed(device);
  await timed("restoring the seed", () => runScene(device, APP_ID, "SeedRestore", { seedUri }));
  // Reading during setup happens at the real time and must stay out of the showcase history.
  writePreferences(device, APP_ID, APP_PREFERENCES, { __APP_STATE_incognito_mode: true });
  // Text recognition is the slowest work a scene waits for, and the restore may have set the system to work again.
  await timed("waiting for the device to settle", () => awaitIdle(device));
  await timed("readying on-page translation", () => runScene(device, APP_ID, "translation.TranslationSetup"));
  writePreferences(device, APP_ID, APP_PREFERENCES, {
    __APP_STATE_incognito_mode: false,
    // Library state is kept per profile; the restore creates the seed's profiles with ids from 1 in plan order.
    ...Object.fromEntries(
      profiles.map((_, index) => [`__APP_STATE_profile_${index + 1}:library_update_last_timestamp`, LIBRARY_UPDATED_AT]),
    ),
  });

  pinShowcaseClock(device);
  await freezeStatusBar(device);
}

function reusedEmulator(): Device {
  if (!attachedSerials().includes(SHOWCASE_SERIAL)) throw new Error("--reuse-emulator needs a run's --keep-emulator first");
  return new Device(SHOWCASE_SERIAL);
}

/** Runs [step], reporting how long it took, so slow steps stand out in a run's output. */
async function timed<T>(label: string, step: () => T | Promise<T>): Promise<T> {
  console.error(label);
  const start = Date.now();
  try {
    return await step();
  } finally {
    console.error(`  ${label}: ${Math.round((Date.now() - start) / 1000)} s`);
  }
}

/**
 * Puts the seed where the app's restore screen can open it: in shared storage, indexed by the media provider and owned
 * by the app, which may then read it without a storage permission.
 */
async function pushSeed(device: Device): Promise<string> {
  device.push(values.seed, SEED_ON_DEVICE);
  device.shell(`content call --method scan_file --uri content://media/external/file --arg ${SEED_ON_DEVICE}`);
  // The provider indexes the file in the background, under its canonical path rather than /sdcard.
  let id: string | undefined;
  for (let attempt = 0; id === undefined && attempt < 40; attempt++) {
    if (attempt > 0) await sleep(250);
    const rows = device.shell(
      `content query --uri content://media/external/file --projection _id --where "_display_name='${SEED_NAME}'"`,
    );
    id = rows.match(/_id=(\d+)/)?.[1];
  }
  if (id === undefined) throw new Error(`The media provider did not index ${SEED_ON_DEVICE}`);
  device.shell(`content update --uri content://media/external/file/${id} --bind owner_package_name:s:${APP_ID}`);
  // The fragment names the file, which the restore screen shows and checks the extension of.
  return `content://media/external/file/${id}#${SEED_NAME}`;
}

async function startContentServer(served: string): Promise<ChildProcess> {
  const catalog = `${EMULATOR_CONTENT_SERVER.replace("10.0.2.2", "127.0.0.1")}/catalog.json`;
  // A server left over from another run would answer in place of this one, with whatever it was serving.
  if (await answers(catalog)) throw new Error(`Something already serves ${catalog}; stop it first`);
  const child = spawn(process.execPath, [join(import.meta.dirname, "..", "content", "serve.ts"), "--root", served], {
    stdio: ["ignore", "ignore", "inherit"],
  });
  for (let attempt = 0; attempt < 50; attempt++) {
    if (child.exitCode !== null) throw new Error(`The content server exited with ${child.exitCode}`);
    if (await answers(catalog)) return child;
    await sleep(200);
  }
  child.kill();
  throw new Error(`The content server did not start on ${catalog}`);
}

async function answers(url: string): Promise<boolean> {
  try {
    return (await fetch(url)).ok;
  } catch {
    return false;
  }
}
