/**
 * Edits an app's database while the app is stopped. Some app state is left out of backups on purpose, so the pipeline
 * writes the rows the screenshots need directly.
 */
import { existsSync, mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { DatabaseSync } from "node:sqlite";
import type { Device } from "./adb.ts";

/**
 * Runs [edit] on a copy of the app's database and writes the copy back. The copy takes the write-ahead log along, and
 * closing it folds the log into the file, so the log left on the device is removed with the rest of it.
 */
export function editDatabase(device: Device, appId: string, file: string, edit: (database: DatabaseSync) => void): void {
  const remote = `/data/data/${appId}/databases/${file}`;
  // Stopped before reading, so nothing the app writes in between is overwritten with the older copy.
  device.forceStop(appId);

  const directory = mkdtempSync(join(tmpdir(), "katari-database-"));
  try {
    const local = join(directory, file);
    device.pull(remote, local);
    for (const suffix of LOG_SUFFIXES) {
      if (device.shell(`[ -f ${remote}${suffix} ] && echo yes || true`).trim() === "yes") {
        device.pull(`${remote}${suffix}`, `${local}${suffix}`);
      }
    }

    const database = new DatabaseSync(local);
    try {
      edit(database);
    } finally {
      database.close();
    }
    if (existsSync(`${local}-wal`)) throw new Error(`The edited copy of ${file} kept its write-ahead log`);

    const staged = `/data/local/tmp/${file}`;
    device.push(local, staged);
    // Writing through the existing file keeps the app's ownership and SELinux label on it.
    device.shell(`cat ${staged} > ${remote} && rm ${staged}`);
    device.shell(LOG_SUFFIXES.map((suffix) => `rm -f ${remote}${suffix}`).join(" && "));
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

const LOG_SUFFIXES = ["-wal", "-shm"];
