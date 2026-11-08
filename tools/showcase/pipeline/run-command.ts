/**
 * Runs a host command to completion and returns its output, failing with everything it printed when it fails, or when
 * it runs past [timeoutMs], if given.
 */
import { execFile, spawnSync } from "node:child_process";

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
  if (result.status !== 0) throw exitError(command, args, result.status, result.stdout + result.stderr);
  return result.stdout;
}

/** As [run], but leaves the pipeline free to do other work while the command runs. */
export function runAsync(
  command: string,
  args: string[],
  env: NodeJS.ProcessEnv = process.env,
  timeoutMs?: number,
): Promise<string> {
  return new Promise((resolve, reject) => {
    const options = {
      encoding: "utf8",
      env,
      maxBuffer: 64 * 1024 * 1024,
      timeout: timeoutMs,
      killSignal: "SIGKILL",
    } as const;
    execFile(command, args, options, (error, stdout, stderr) => {
      if (error === null) {
        resolve(stdout);
      } else if (error.killed && timeoutMs !== undefined) {
        reject(new Error(`${command} ${args.join(" ")} did not finish within ${timeoutMs / 1000} s`));
      } else {
        reject(typeof error.code === "number" ? exitError(command, args, error.code, stdout + stderr) : error);
      }
    });
  });
}

function exitError(command: string, args: string[], status: number | null, output: string): Error {
  return new Error(`${command} ${args.join(" ")} exited with ${status}\n${output}`);
}
