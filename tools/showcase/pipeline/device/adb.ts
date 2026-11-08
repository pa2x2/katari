/** One device reached through adb, addressed by serial so another connected phone or emulator is never touched. */
import { run } from "../run-command.ts";

export class Device {
  readonly serial: string;

  constructor(serial: string) {
    this.serial = serial;
  }

  adb(...args: string[]): string {
    return this.adbWithin(COMMAND_TIMEOUT_MS, ...args);
  }

  /** For a command that may take longer than a single step should, such as a whole scene. */
  adbWithin(timeoutMs: number, ...args: string[]): string {
    return run("adb", ["-s", this.serial, ...args], process.env, timeoutMs);
  }

  shell(command: string): string {
    return this.adb("shell", command);
  }

  push(local: string, remote: string): void {
    this.adb("push", local, remote);
  }

  pull(remote: string, local: string): void {
    this.adb("pull", remote, local);
  }

  forceStop(appId: string): void {
    this.shell(`am force-stop ${appId}`);
  }
}

/** Serials of every device adb currently lists as attached, emulators included. */
export function attachedSerials(): string[] {
  return run("adb", ["devices"], process.env, COMMAND_TIMEOUT_MS)
    .split("\n")
    .slice(1)
    .map((line) => line.split("\t"))
    .filter(([serial, state]) => serial && state === "device")
    .map(([serial]) => serial!);
}

/**
 * A device that stops answering would otherwise hold a run until the workflow's own time limit cancels it, which skips
 * the steps that keep what went wrong.
 */
const COMMAND_TIMEOUT_MS = 5 * 60_000;
