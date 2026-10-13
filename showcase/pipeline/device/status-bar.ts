/**
 * Puts System UI in demo mode, which draws a fixed status bar: the showcase time, a full battery and Wi-Fi signal, and
 * no notification icons. Otherwise the minutes the run takes, charging, and setup notifications show up as pixel changes.
 */
import { setTimeout as sleep } from "node:timers/promises";
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import type { Device } from "./adb.ts";

const ROTATION_TIMEOUT_MS = 10_000;

export async function freezeStatusBar(device: Device): Promise<void> {
  await relayOutStatusBar(device);
  device.shell("settings put global sysui_demo_allowed 1");
  const time = new Date(SHOWCASE_NOW);
  const hhmm = `${String(time.getUTCHours()).padStart(2, "0")}${String(time.getUTCMinutes()).padStart(2, "0")}`;
  const commands = [
    "enter",
    `clock -e hhmm ${hhmm}`,
    "battery -e level 100 -e plugged false",
    "network -e wifi show -e level 4 -e fully true",
    // Demo mode labels mobile data 3G whatever data type it is given.
    "network -e mobile hide",
    "notifications -e visible false",
  ];
  for (const command of commands) {
    const [name, ...extras] = command.split(" ");
    device.shell(`am broadcast -a com.android.systemui.demo -e command ${name} ${extras.join(" ")}`);
  }
}

/**
 * A wiped emulator's System UI sometimes lays the status bar out before the display's cutout is in place, 5 pixels
 * shorter than it does from then on, and keeps it so until the screen next turns. Every still of such a run would differ
 * from another run's. Turning the screen and back has System UI lay the bar out again, and leaves it locked upright.
 */
async function relayOutStatusBar(device: Device): Promise<void> {
  for (const rotation of [1, 0]) {
    device.shell(`cmd window user-rotation lock ${rotation}`);
    const deadline = Date.now() + ROTATION_TIMEOUT_MS;
    while (!device.shell("dumpsys window displays").includes(`mCurrentRotation=ROTATION_${rotation * 90}`)) {
      if (Date.now() > deadline) throw new Error(`The emulator never turned to rotation ${rotation}`);
      await sleep(200);
    }
  }
}
