import { execFileSync, spawn } from "node:child_process";
import { createWriteStream } from "node:fs";
import { pipeline } from "node:stream/promises";

// Node has no zip reader; `unzip` is on every machine the pipeline runs on and streams members of any size.

export function readMember(archive: string, member: string): Buffer {
  return execFileSync("unzip", ["-p", archive, member], { maxBuffer: 64 * 1024 * 1024 });
}

export async function extractMember(archive: string, member: string, target: string): Promise<void> {
  const unzip = spawn("unzip", ["-p", archive, member], { stdio: ["ignore", "pipe", "inherit"] });
  const exited = new Promise<void>((resolve, reject) =>
    unzip.on("close", (code) => (code === 0 ? resolve() : reject(new Error(`unzip ${member} exited with ${code}`)))),
  );
  await pipeline(unzip.stdout, createWriteStream(target));
  await exited;
}
