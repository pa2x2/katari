/**
 * Waits for a freshly wiped emulator to finish the work it starts on its own. After the apps are installed, Play services
 * registers each of them with the credential manager while the system finishes its first-boot setup, for minutes on
 * end; on a CI runner's four cores that slows the app's text recognition several times over, by a different amount on
 * every run, and makes recorded clips stutter.
 */
import { setTimeout as sleep } from "node:timers/promises";
import type { Device } from "./adb.ts";

const SAMPLE_MS = 5_000;
// The share of all cores in use below which the device counts as idle. An idle emulator drawing in software stays
// well under it.
const IDLE_BUSY_SHARE = 0.25;
const IDLE_SAMPLES_IN_A_ROW = 2;
const MAX_WAIT_MS = 5 * 60_000;

export async function awaitIdle(device: Device): Promise<void> {
  const deadline = Date.now() + MAX_WAIT_MS;
  let idleSamples = 0;
  let previous = cpuTimes(device);
  while (idleSamples < IDLE_SAMPLES_IN_A_ROW) {
    await sleep(SAMPLE_MS);
    const current = cpuTimes(device);
    const total = current.total - previous.total;
    const busy = total === 0 ? 0 : 1 - (current.idle - previous.idle) / total;
    previous = current;
    idleSamples = busy < IDLE_BUSY_SHARE ? idleSamples + 1 : 0;
    if (idleSamples < IDLE_SAMPLES_IN_A_ROW && Date.now() > deadline) {
      // Slower scenes are still likelier to pass than a run that gives up here.
      console.error(`  the device was still ${Math.round(busy * 100)}% busy after ${MAX_WAIT_MS / 60_000} minutes`);
      return;
    }
  }
}

/** Time all cores have spent since boot, and the part of it spent idle or waiting on storage. */
function cpuTimes(device: Device): { total: number; idle: number } {
  const [user, nice, system, idle, iowait, irq, softirq, steal] = device
    .shell("head -n 1 /proc/stat")
    .trim()
    .split(/\s+/)
    .slice(1, 9)
    .map(Number) as [number, number, number, number, number, number, number, number];
  return { total: user + nice + system + idle + iowait + irq + softirq + steal, idle: idle + iowait };
}
