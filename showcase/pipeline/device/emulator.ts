/**
 * The dedicated showcase emulator: a rootable Google APIs image, so the pipeline can pin the date and write app state,
 * wiped on every boot, so no run inherits another's state.
 */
import { type ChildProcess, spawn } from "node:child_process";
import { closeSync, existsSync, mkdirSync, mkdtempSync, openSync, readFileSync } from "node:fs";
import { homedir, tmpdir } from "node:os";
import { join } from "node:path";
import { setTimeout as sleep } from "node:timers/promises";
import { run } from "../run-command.ts";
import { attachedSerials, Device } from "./adb.ts";

const AVD = "Katari_Showcase";
const SYSTEM_IMAGE = "system-images;android-36.1;google_apis;x86_64";
const DEVICE_PROFILE = "pixel_9a";
export const SHOWCASE_SERIAL = "emulator-5554";
const BOOT_TIMEOUT_MS = 10 * 60_000;
const SHUTDOWN_TIMEOUT_MS = 60_000;
const ROOT_TIMEOUT_MS = 2 * 60_000;
// Well inside the 5 s an app has to answer input, so the setting is written before the launcher can miss its deadline.
const SETTINGS_POLL_MS = 500;
const BOOT_FAILURE_OUTPUT_LINES = 40;
// Passed to avdmanager and the emulator alike, so both use the folder this script checks. Without it, avdmanager
// prefers $XDG_CONFIG_HOME/.android when that exists, as it does on GitHub's runners, and the emulator never looks there.
const AVD_HOME = process.env.ANDROID_AVD_HOME ?? join(homedir(), ".android", "avd");
const AVD_ENV = { ...process.env, ANDROID_AVD_HOME: AVD_HOME };

export async function bootShowcaseEmulator(): Promise<Device> {
  // Scenes find views by text and tap by screen fractions, so a second device would only ever be touched by mistake.
  const running = attachedSerials().filter((serial) => serial.startsWith("emulator-"));
  if (running.length > 0) throw new Error(`Stop the running emulators first: ${running.join(", ")}`);
  // The emulator finds an AVD by its .ini, which avdmanager writes beside the .avd folder.
  const avdIni = join(AVD_HOME, `${AVD}.ini`);
  if (!existsSync(avdIni)) {
    // avdmanager ignores an ANDROID_AVD_HOME that does not exist yet.
    mkdirSync(AVD_HOME, { recursive: true });
    const command = `echo no | avdmanager create avd --force -n ${AVD} -k '${SYSTEM_IMAGE}' -d ${DEVICE_PROFILE} 2>&1`;
    const output = run("sh", ["-c", command], AVD_ENV);
    if (!existsSync(avdIni)) throw new Error(`avdmanager created no ${avdIni}:\n${output}`);
  }
  // In a file, since a kept emulator outlives this script and anything reading its output.
  const log = join(mkdtempSync(join(tmpdir(), "katari-showcase-")), "emulator.log");
  const output = openSync(log, "w");
  const emulator = spawn(
    "emulator",
    [
      "-avd", AVD,
      "-no-window", "-no-audio", "-no-boot-anim", "-no-snapshot", "-wipe-data",
      // System services keep the time zone they boot with.
      "-timezone", "Etc/UTC",
      // Software rendering, as runners have no GPU. The other software modes break apps: the legacy GLES translator
      // (swiftshader_indirect, lavapipe) crashes the emulator on the recap's drifting gradients, and apps drawing through
      // the guest's Vulkan driver instead hang now and then in a frame that never finishes, which leaves their window
      // without an accessibility tree. ANGLE over SwiftShader draws both.
      "-gpu", "swangle",
    ],
    { detached: true, env: AVD_ENV, stdio: ["ignore", output, output] },
  );
  closeSync(output);
  emulator.unref();

  const device = new Device(SHOWCASE_SERIAL);
  try {
    await hideErrorDialogs(device, emulator);
    await awaitBoot(device, emulator);
    await restartAdbdAsRoot(device, emulator);
  } catch (error) {
    // Left running, it would stop every later run from starting.
    emulator.kill();
    const lines = readFileSync(log, "utf8").trimEnd().split("\n").slice(-BOOT_FAILURE_OUTPUT_LINES).join("\n");
    throw new Error(`${(error as Error).message}. The emulator's last output, all of it in ${log}:\n${lines}`);
  }
  // Downloads that wait for an unmetered network treat Wi-Fi as unmetered and mobile data as metered.
  device.shell("svc wifi enable; svc data disable");
  // A fresh device explains full-screen mode over the first app that hides the system bars.
  device.shell("settings put secure immersive_mode_confirmations confirmed");
  return device;
}

export async function shutDown(device: Device): Promise<void> {
  device.adb("emu", "kill");
  // The emulator stays attached while it shuts down, and the next run refuses to start beside it.
  const deadline = Date.now() + SHUTDOWN_TIMEOUT_MS;
  while (attachedSerials().includes(device.serial)) {
    if (Date.now() > deadline) throw new Error(`${AVD} did not shut down`);
    await sleep(1_000);
  }
}

/**
 * `adb root` only asks adbd to restart, and a device that has just booted can drop the request, the emulator still
 * configuring it over adb: CI saw "unable to connect for root: closed". The old adbd may also answer for a moment after a
 * request it took. So it asks until a shell runs as root, rather than trusting the command's exit status.
 */
async function restartAdbdAsRoot(device: Device, emulator: ChildProcess): Promise<void> {
  const deadline = Date.now() + ROOT_TIMEOUT_MS;
  let refusal: unknown;
  while (Date.now() < deadline) {
    try {
      device.adb("root");
    } catch (error) {
      refusal = error;
    }
    await awaitBoot(device, emulator);
    try {
      if (device.shell("id -u").trim() === "0") return;
    } catch {
      // adbd went offline to restart after answering for boot_completed.
    }
    await sleep(2_000);
  }
  throw new Error(`adbd did not restart as root within ${ROOT_TIMEOUT_MS / 60_000} minutes. ${refusal ?? ""}`);
}

/**
 * A device drawing in software is busy enough for the launcher to miss its input deadline while it starts, and the
 * system's "isn't responding" dialog would stay over the app; without dialogs, the system restarts the launcher instead.
 * A dialog raised before the setting stays up, and CI saw the launcher's raised before boot completed, so the setting is
 * written as soon as the settings service takes it: system_server installs its providers before it starts the launcher.
 */
async function hideErrorDialogs(device: Device, emulator: ChildProcess): Promise<void> {
  const deadline = Date.now() + BOOT_TIMEOUT_MS;
  while (Date.now() < deadline) {
    if (emulator.exitCode !== null || emulator.signalCode !== null) {
      throw new Error(`${AVD} exited with ${emulator.exitCode ?? emulator.signalCode} before it booted`);
    }
    try {
      device.shell("settings put global hide_error_dialogs 1");
      if (device.shell("settings get global hide_error_dialogs").trim() === "1") return;
    } catch {
      // adb reports the emulator as offline until it accepts connections, and the settings service starts later still.
    }
    await sleep(SETTINGS_POLL_MS);
  }
  throw new Error(`${AVD} never took the setting that hides error dialogs`);
}

async function awaitBoot(device: Device, emulator: ChildProcess): Promise<void> {
  const deadline = Date.now() + BOOT_TIMEOUT_MS;
  while (Date.now() < deadline) {
    if (emulator.exitCode !== null || emulator.signalCode !== null) {
      throw new Error(`${AVD} exited with ${emulator.exitCode ?? emulator.signalCode} before it booted`);
    }
    try {
      if (device.shell("getprop sys.boot_completed").trim() === "1") return;
    } catch {
      // adb reports the emulator as offline until it accepts connections.
    }
    await sleep(2_000);
  }
  throw new Error(`${AVD} did not boot within ${BOOT_TIMEOUT_MS / 60_000} minutes`);
}
