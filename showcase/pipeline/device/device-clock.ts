/**
 * The emulator's clock. Setup downloads text recognition models over HTTPS, which fails certificate checks at the
 * showcase date, so it runs at the real time; the scenes run at the pinned showcase moment.
 */
import { SHOWCASE_NOW } from "../../seed/showcase-clock.ts";
import type { Device } from "./adb.ts";

export function useRealClock(device: Device): void {
  setClock(device, Date.now());
}

export function pinShowcaseClock(device: Device): void {
  setClock(device, SHOWCASE_NOW);
}

function setClock(device: Device, time: number): void {
  device.shell("settings put global auto_time 0");
  const date = new Date(time);
  const two = (value: number) => String(value).padStart(2, "0");
  // Toybox reads MMDDhhmmYYYY.ss; other forms it accepts silently land in another year.
  const stamp =
    two(date.getUTCMonth() + 1) +
    two(date.getUTCDate()) +
    two(date.getUTCHours()) +
    two(date.getUTCMinutes()) +
    date.getUTCFullYear() +
    `.${two(date.getUTCSeconds())}`;
  device.shell(`date ${stamp}`);
}
