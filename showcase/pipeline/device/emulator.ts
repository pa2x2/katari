/**
 * The dedicated showcase emulator: a rootable Google APIs image, so the pipeline can pin the date and write app state,
 * wiped on every boot, so no run inherits another's state.
 */
import { type ChildProcess, spawn } from "node:child_process";
import { closeSync, existsSync, mkdtempSync, openSync, readFileSync } from "node:fs";
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
const BOOT_FAILURE_OUTPUT_LINES = 40;
// Passed to avdmanager and the emulator alike: by default avdmanager finds the AVD folder under the account's home
// directory and the emulator under $HOME, and on CI runners the two differ.
const AVD_HOME = process.env.ANDROID_AVD_HOME ?? join(homedir(), ".android", "avd");
const AVD_ENV = { ...process.env, ANDROID_AVD_HOME: AVD_HOME };

export async function bootShowcaseEmulator(): Promise<Device> {
  // Scenes find views by text and tap by screen fractions, so a second device would only ever be touched by mistake.
  const running = attachedSerials().filter((serial) => serial.startsWith("emulator-"));
  if (running.length > 0) throw new Error(`Stop the running emulators first: ${running.join(", ")}`);
  if (!existsSync(join(AVD_HOME, `${AVD}.avd`))) {
    run("sh", ["-c", `echo no | avdmanager create avd -n ${AVD} -k '${SYSTEM_IMAGE}' -d ${DEVICE_PROFILE}`], AVD_ENV);
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
      // Host GPU rendering crashes the emulator's render thread on some drivers.
      "-gpu", "swiftshader_indirect",
    ],
    { detached: true, env: AVD_ENV, stdio: ["ignore", output, output] },
  );
  closeSync(output);
  emulator.unref();

  const device = new Device(SHOWCASE_SERIAL);
  try {
    await awaitBoot(device, emulator);
    device.adb("root");
    await awaitBoot(device, emulator);
  } catch (error) {
    // Left running, it would stop every later run from starting.
    emulator.kill();
    const lines = readFileSync(log, "utf8").trimEnd().split("\n").slice(-BOOT_FAILURE_OUTPUT_LINES).join("\n");
    throw new Error(`${(error as Error).message}. The emulator's last output, all of it in ${log}:\n${lines}`);
  }
  // Downloads that wait for an unmetered network treat Wi-Fi as unmetered and mobile data as metered.
  device.shell("svc wifi enable; svc data disable");
  // The emulator's software GLES translator crashes the whole emulator on the recap's drifting gradients; apps started
  // from here on draw through Vulkan instead.
  device.shell("setprop debug.hwui.renderer skiavk");
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
