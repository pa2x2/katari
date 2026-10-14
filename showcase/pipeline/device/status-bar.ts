/**
 * Puts System UI in demo mode, which draws a fixed status bar: the showcase time, a full battery and Wi-Fi signal, and
 * no notification icons. Otherwise the minutes the run takes, charging, and setup notifications show up as pixel changes.
 */
import { setTimeout as sleep } from "node:timers/promises";
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import type { Device } from "./adb.ts";

const ROTATION_TIMEOUT_MS = 10_000;
const WIFI_TIMEOUT_MS = 60_000;
const WIFI_RETRY_MS = 1_000;
const WIFI_COMMAND = "network -e wifi show -e level 4 -e fully true";
// The status bar's description of its Wi-Fi icon at full signal, in the emulator's English.
const WIFI_ICON = 'content-desc="Wifi signal full."';

export async function freezeStatusBar(device: Device): Promise<void> {
  await relayOutStatusBar(device);
  device.shell("settings put global sysui_demo_allowed 1");
  const time = new Date(SHOWCASE_NOW);
  const hhmm = `${String(time.getUTCHours()).padStart(2, "0")}${String(time.getUTCMinutes()).padStart(2, "0")}`;
  const commands = [
    "enter",
    `clock -e hhmm ${hhmm}`,
    "battery -e level 100 -e plugged false",
    // Demo mode labels mobile data 3G whatever data type it is given.
    "network -e mobile hide",
    "notifications -e visible false",
  ];
  for (const command of commands) sendDemoCommand(device, command);
  await showWifi(device);
}

function sendDemoCommand(device: Device, command: string): void {
  const [name, ...extras] = command.split(" ");
  device.shell(`am broadcast -a com.android.systemui.demo -e command ${name} ${extras.join(" ")}`);
}

/**
 * System UI's Wi-Fi icon starts listening for demo commands only after demo mode has started, and drops a command that
 * arrives before then, which a busy device makes likely. Without Wi-Fi, System UI shows its satellite icon in its
 * place, as CI's stills did now and then. So the command is sent until the status bar describes the Wi-Fi icon.
 */
async function showWifi(device: Device): Promise<void> {
  const deadline = Date.now() + WIFI_TIMEOUT_MS;
  while (true) {
    sendDemoCommand(device, WIFI_COMMAND);
    await sleep(WIFI_RETRY_MS);
    if (windowTrees(device).includes(WIFI_ICON)) return;
    if (Date.now() > deadline) throw new Error("The status bar never showed the demo Wi-Fi icon");
  }
}

/** Every window's accessibility tree, the status bar's among them, as UiAutomator writes it. */
function windowTrees(device: Device): string {
  const file = "/data/local/tmp/showcase-windows.xml";
  try {
    return device.shell(`uiautomator dump --windows ${file} > /dev/null && cat ${file}`);
  } catch {
    // UiAutomator gives up on a screen that does not go idle; the next attempt dumps it again.
    return "";
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
