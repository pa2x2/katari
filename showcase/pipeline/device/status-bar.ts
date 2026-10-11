/**
 * Puts System UI in demo mode, which draws a fixed status bar: the showcase time, a full battery and Wi-Fi signal, and
 * no notification icons. Otherwise the minutes the run takes, charging, and setup notifications show up as pixel changes.
 */
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import type { Device } from "./adb.ts";

export function freezeStatusBar(device: Device): void {
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
