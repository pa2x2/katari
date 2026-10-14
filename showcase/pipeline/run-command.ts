/**
 * Runs a host command to completion and returns its output, failing with everything it printed when it fails, or when
 * it runs past [timeoutMs], if given.
 */
import { spawnSync } from "node:child_process";

export function run(command: string, args: string[], env: NodeJS.ProcessEnv = process.env, timeoutMs?: number): string {
  const result = spawnSync(command, args, {
    encoding: "utf8",
    env,
    maxBuffer: 64 * 1024 * 1024,
    timeout: timeoutMs,
    killSignal: "SIGKILL",
  });
  if ((result.error as NodeJS.ErrnoException | undefined)?.code === "ETIMEDOUT") {
    throw new Error(`${command} ${args.join(" ")} did not finish within ${timeoutMs! / 1000} s`);
  }
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(" ")} exited with ${result.status}\n${result.stdout}${result.stderr}`);
  }
  return result.stdout;
}
