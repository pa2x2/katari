/**
 * Puts System UI in demo mode, which draws a fixed status bar: the showcase time, a full battery and Wi-Fi signal, and
 * no notification icons. Otherwise the minutes the run takes, charging, and setup notifications show up as pixel changes.
 */
import { setTimeout as sleep } from "node:timers/promises";
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import type { Device } from "./adb.ts";

const ROTATION_TIMEOUT_MS = 10_000;
// Locally the bar takes up to a second after the screen is upright again to be laid out for the cutout.
const LAYOUT_TIMEOUT_MS = 10_000;
const LAYOUT_POLL_MS = 250;
const LAYOUT_ATTEMPTS = 3;
const ICONS_TIMEOUT_MS = 60_000;
const ICONS_RETRY_MS = 1_000;

/** A demo command that sets an icon, and the status bar's description of that icon in the emulator's English. */
interface IconCommand {
  command: string;
  shows: string;
}

const ICON_COMMANDS: IconCommand[] = [
  { command: "battery -e level 100 -e plugged false", shows: 'content-desc="Battery 100 percent."' },
  { command: "network -e wifi show -e level 4 -e fully true", shows: 'content-desc="Wifi signal full."' },
];

export async function freezeStatusBar(device: Device): Promise<void> {
  await relayOutStatusBar(device);
  device.shell("settings put global sysui_demo_allowed 1");
  const time = new Date(SHOWCASE_NOW);
  const hhmm = `${String(time.getUTCHours()).padStart(2, "0")}${String(time.getUTCMinutes()).padStart(2, "0")}`;
  const commands = [
    "enter",
    `clock -e hhmm ${hhmm}`,
    // Demo mode labels mobile data 3G whatever data type it is given.
    "network -e mobile hide",
    "notifications -e visible false",
  ];
  for (const command of commands) sendDemoCommand(device, command);
  await showIcons(device);
}

function sendDemoCommand(device: Device, command: string): void {
  const [name, ...extras] = command.split(" ");
  device.shell(`am broadcast -a com.android.systemui.demo -e command ${name} ${extras.join(" ")}`);
}

/**
 * A busy device makes System UI lose the icons' demo commands. Its Wi-Fi icon starts listening for them only after demo
 * mode has started and drops one that arrives before then, leaving its satellite icon in Wi-Fi's place. Its battery
 * ignores a command that comes before it counts demo mode as started, and stops taking in the real battery only once it
 * does, so a real update still on its way brings back the emulator's charger. CI's stills showed both now and then. So
 * each command is sent until the status bar describes the icon it sets.
 */
async function showIcons(device: Device): Promise<void> {
  const deadline = Date.now() + ICONS_TIMEOUT_MS;
  let missing = ICON_COMMANDS;
  while (true) {
    for (const icon of missing) sendDemoCommand(device, icon.command);
    await sleep(ICONS_RETRY_MS);
    const trees = windowTrees(device);
    missing = missing.filter((icon) => !trees.includes(icon.shows));
    if (missing.length === 0) return;
    if (Date.now() > deadline) {
      throw new Error(`The status bar never showed ${missing.map((icon) => icon.shows).join(" or ")}`);
    }
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
 * The status bar's height and the top inset of the display's cutout, in pixels, as the window manager has them. A bar
 * laid out for the cutout is at least as tall as its inset.
 */
interface StatusBarLayout {
  bar: number;
  cutout: number;
}

function statusBarLayout(device: Device): StatusBarLayout {
  const windows = device.shell("dumpsys window");
  const bar = windows.match(/type=statusBars frame=\[\d+,(\d+)\]\[\d+,(\d+)\]/);
  const cutout = windows.match(/mDisplayCutout=DisplayCutout\{insets=Rect\(\d+, (\d+) - /);
  if (bar === null || cutout === null) throw new Error("The window manager listed no status bar or display cutout");
  return { bar: Number(bar[2]) - Number(bar[1]), cutout: Number(cutout[1]) };
}

function describe(layout: StatusBarLayout): string {
  return `a ${layout.bar} px status bar for a ${layout.cutout} px cutout`;
}

/**
 * Throws when the status bar is shorter than the cutout, as a scene's stills would then show it. [moment] says when, such
 * as after which scene.
 */
export function checkStatusBar(device: Device, moment: string): void {
  const layout = statusBarLayout(device);
  if (layout.bar < layout.cutout) throw new Error(`The phone had ${describe(layout)} ${moment}`);
}

/**
 * Lays the status bar out again when it is shorter than the cutout, saying so; a scene's stills then show the same bar
 * whatever happened to it before.
 */
export async function restoreStatusBar(device: Device, moment: string): Promise<void> {
  const layout = statusBarLayout(device);
  if (layout.bar >= layout.cutout) return;
  console.log(`  the phone had ${describe(layout)} ${moment}; laying it out again`);
  await relayOutStatusBar(device);
}

/**
 * A few seconds after a wiped emulator boots, the device's overlay gives the display its cutout, and System UI then most
 * often lays the status bar out shorter than the cutout (137 pixels for the showcase AVD's 142) and keeps it so until
 * the screen next turns. A run whose bar stayed short would differ from every other in all its stills. Turning the
 * screen and back has System UI lay the bar out for the cutout, and leaves it locked upright. The bar is laid out anew
 * only a moment after the screen is upright, so a turn counts once the bar is as tall as the cutout.
 *
 * The screen turns whatever is in front: the launcher, there after a restore, keeps its home screen upright. Apps choose
 * their orientation again afterwards, which the player needs to turn to landscape.
 */
async function relayOutStatusBar(device: Device): Promise<void> {
  let layout = statusBarLayout(device);
  for (let attempt = 1; attempt <= LAYOUT_ATTEMPTS; attempt++) {
    device.shell("cmd window fixed-to-user-rotation enabled");
    try {
      await turnTo(device, 1);
      await turnTo(device, 0);
    } finally {
      device.shell("cmd window fixed-to-user-rotation default");
    }
    const deadline = Date.now() + LAYOUT_TIMEOUT_MS;
    while (true) {
      layout = statusBarLayout(device);
      if (layout.bar >= layout.cutout) {
        console.log(`  the phone has ${describe(layout)}`);
        return;
      }
      if (Date.now() > deadline) break;
      await sleep(LAYOUT_POLL_MS);
    }
  }
  throw new Error(`Turning the screen ${LAYOUT_ATTEMPTS} times left ${describe(layout)}`);
}

async function turnTo(device: Device, rotation: number): Promise<void> {
  device.shell(`cmd window user-rotation lock ${rotation}`);
  const deadline = Date.now() + ROTATION_TIMEOUT_MS;
  while (!device.shell("dumpsys window displays").includes(`mCurrentRotation=ROTATION_${rotation * 90}`)) {
    if (Date.now() > deadline) throw new Error(`The emulator never turned to rotation ${rotation}`);
    await sleep(200);
  }
}
