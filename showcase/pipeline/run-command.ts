/** Runs a host command to completion and returns its output, failing with everything it printed when it fails. */
import { spawnSync } from "node:child_process";

export function run(command: string, args: string[]): string {
  const result = spawnSync(command, args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} exited with ${result.status}\n${result.stdout}${result.stderr}`);
  }
  return result.stdout;
}
