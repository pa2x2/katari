/**
 * The phone's part of a scene group: a freshly wiped emulator set up for the scenes (or, for --reuse-emulator, the one
 * a run kept), the scenes run on it as instrumentation, and what they leave pulled into the run's folder.
 *
 * Setup runs at the real time, because the text recognition models download over HTTPS: it restores the seed backup
 * (seed/generate.ts) into the benchmark build and, for the translation scenes, readies on-page translation. Then the
 * clock moves to the showcase moment and the scenes run.
 */
import { writeFileSync } from "node:fs";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";
import { CONTENT_SERVER_PORT } from "../../content/content-server.ts";
import { profiles } from "../../seed/library-plan.ts";
import { LIBRARY_UPDATED_AT, SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import { timed } from "../timed.ts";
import { encodeForWeb } from "../web-clip.ts";
import { attachedSerials, Device } from "./adb.ts";
import { pinShowcaseClock, useRealClock } from "./device-clock.ts";
import { awaitIdle } from "./device-idle.ts";
import { bootShowcaseEmulator, SHOWCASE_SERIAL, shutDown } from "./emulator.ts";
import { CAPTURE_DIRECTORY, FAILURE_DIRECTORY, runInstrumentation } from "./instrumentation.ts";
import { installOfflineTranslator, startOfflineTranslator } from "./offline-translator.ts";
import { writePreferences } from "./shared-preferences.ts";
import { freezeStatusBar } from "./status-bar.ts";

const APP_ID = "app.katari.benchmark";
const APP_PREFERENCES = `${APP_ID}_preferences.xml`;
const RECAP_SHARE_IMAGE = `/data/data/${APP_ID}/cache/shared_image/statistics_recap.png`;
const SEED_NAME = "showcase.tachibk";
const SEED_ON_DEVICE = `/sdcard/Download/${SEED_NAME}`;
// The app's name for the recap of the year so far, which it offers in December, as at the showcase moment.
const YEAR_RECAP_EDITION = `${new Date(SHOWCASE_NOW).getUTCFullYear()}-so-far`;
// App state is kept per profile; the restore creates the seed's profiles with ids from 1 in plan order.
const PERSONAL_PROFILE_ID = profiles.findIndex((profile) => profile.name === "Personal") + 1;

export interface PhoneOptions {
  appApk: string;
  extensionApk: string;
  captureApk: string;
  seed: string;
  /** Where the scenes' stills and clips go. */
  out: string;
  /** Uses the emulator a run with keep left, already set up, rather than a fresh one. */
  reuse: boolean;
  /** Leaves the emulator running when the session closes. */
  keep: boolean;
}

export interface PhoneScenes {
  /** Whether a scene translates with Offline Translator, which setup then installs and readies on-page translation for. */
  translates: boolean;
  /** The phone scenes that find the Personal profile's year recap opened; see scenes.ts. */
  yearRecapOpened: string[];
  /** The scene that records the recap clip and leaves the image the recap shares. */
  recap: string;
}

export class PhoneSession {
  private readonly device: Device;
  private readonly options: PhoneOptions;
  private readonly scenes: PhoneScenes;
  private readonly ran: string[] = [];

  private constructor(device: Device, options: PhoneOptions, scenes: PhoneScenes) {
    this.device = device;
    this.options = options;
    this.scenes = scenes;
  }

  /** Boots and sets up the emulator, or takes the one a run kept. On failure, saves why and shuts it down. */
  static async open(options: PhoneOptions, scenes: PhoneScenes): Promise<PhoneSession> {
    const device = options.reuse ? reusedEmulator() : await timed("booting the emulator", bootShowcaseEmulator);
    const session = new PhoneSession(device, options, scenes);
    try {
      // The apps reach the content server on the emulator's loopback, as the desktop app reaches it on the host's.
      device.reversePort(CONTENT_SERVER_PORT);
      if (!options.reuse) await session.prepare();
      device.shell(`rm -rf ${CAPTURE_DIRECTORY} ${FAILURE_DIRECTORY}`);
      await timed("waiting for the device to settle", () => awaitIdle(device));
    } catch (error) {
      session.saveDiagnostics();
      await session.close();
      throw error;
    }
    return session;
  }

  async run(scene: string): Promise<void> {
    // Times shown relative to now, such as "opened 27 minutes ago", would otherwise depend on how long the scenes
    // before took, and cross a minute in some runs but not in others.
    pinShowcaseClock(this.device);
    // Every scene starts the app afresh, so it reads this, whatever the scenes before it opened.
    writePreferences(this.device, APP_ID, APP_PREFERENCES, {
      [`__APP_STATE_profile_${PERSONAL_PROFILE_ID}:statistics_last_opened_year_recap`]:
        this.scenes.yearRecapOpened.includes(scene) ? YEAR_RECAP_EDITION : "",
    });
    runInstrumentation(this.device, APP_ID, scene);
    this.ran.push(scene);
  }

  /** Pulls what the scenes left into the run's folder, with the recap clip made fit for the site. */
  collect(): void {
    this.device.pull(`${CAPTURE_DIRECTORY}/.`, this.options.out);
    if (this.ran.includes(this.scenes.recap)) {
      this.device.pull(RECAP_SHARE_IMAGE, join(this.options.out, "recap-share.png"));
      encodeForWeb(join(this.options.out, "recap.mp4"));
    }
  }

  /** Keeps what explains a failed run beside its captures. The device may be gone by now, so each part is optional. */
  saveDiagnostics(): void {
    try {
      writeFileSync(join(this.options.out, "logcat.txt"), this.device.adb("logcat", "-d"));
    } catch (error) {
      console.error(`the device log could not be saved: ${error}`);
    }
    try {
      this.device.pull(`${FAILURE_DIRECTORY}/.`, this.options.out);
    } catch {
      // The failure happened before any scene could leave a screenshot or report.
    }
    console.error(`the device log and any screenshots and reports of the failure are in ${this.options.out}`);
  }

  /** Shuts the emulator down unless the session keeps it; a failure to is reported without replacing another error. */
  async close(): Promise<void> {
    if (this.options.keep) return;
    await shutDown(this.device).catch((error) => console.error(`the emulator did not shut down: ${error}`));
  }

  /** Installs the apps and restores the seed, and readies on-page translation when a scene translates. */
  private async prepare(): Promise<void> {
    const device = this.device;
    useRealClock(device);
    await timed("installing the apps", async () => {
      device.adb("install", "-r", this.options.appApk);
      // The seed restore scene waits for the notification the app posts once the restore is done.
      device.shell(`pm grant ${APP_ID} android.permission.POST_NOTIFICATIONS`);
      // Without it, the extension list opens with a banner asking for it.
      device.shell(`appops set ${APP_ID} REQUEST_INSTALL_PACKAGES allow`);
      device.adb("install", "-r", this.options.extensionApk);
      device.adb("install", "-r", "-t", this.options.captureApk);
      if (this.scenes.translates) {
        await installOfflineTranslator(device);
        startOfflineTranslator(device);
      }
    });

    // The apps just installed keep the system busy for minutes, which slows the screens the seed restore taps through.
    await timed("waiting for the device to settle", () => awaitIdle(device));
    const seedUri = await this.pushSeed();
    await timed("restoring the seed", () => runInstrumentation(device, APP_ID, "SeedRestore", { seedUri }));
    if (this.scenes.translates) {
      // Text recognition is the slowest work a scene waits for, and the restore may have set the system to work again.
      await timed("waiting for the device to settle", () => awaitIdle(device));
      await timed("readying on-page translation", () =>
        runInstrumentation(device, APP_ID, "translation.TranslationSetup"),
      );
    }
    writePreferences(device, APP_ID, APP_PREFERENCES, {
      // Translation setup reads in incognito, which shows a banner over every screen.
      __APP_STATE_incognito_mode: false,
      ...Object.fromEntries(
        profiles.map((_, index) => [`__APP_STATE_profile_${index + 1}:library_update_last_timestamp`, LIBRARY_UPDATED_AT]),
      ),
    });

    pinShowcaseClock(device);
    await freezeStatusBar(device);
  }

  /**
   * Puts the seed where the app's restore screen can open it: in shared storage, indexed by the media provider and
   * owned by the app, which may then read it without a storage permission.
   */
  private async pushSeed(): Promise<string> {
    const device = this.device;
    device.push(this.options.seed, SEED_ON_DEVICE);
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
}

function reusedEmulator(): Device {
  if (!attachedSerials().includes(SHOWCASE_SERIAL)) throw new Error("--reuse-emulator needs a run's --keep-emulator first");
  return new Device(SHOWCASE_SERIAL);
}
