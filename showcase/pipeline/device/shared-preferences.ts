/**
 * Writes entries into an app's shared preferences file while the app is stopped. Some app state is left out of backups
 * on purpose, so the pipeline sets what the screenshots need directly.
 */
import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import type { Device } from "./adb.ts";

export type PreferenceValue = boolean | number | string;

export function writePreferences(
  device: Device,
  appId: string,
  file: string,
  entries: Record<string, PreferenceValue>,
): void {
  const remote = `/data/data/${appId}/shared_prefs/${file}`;
  const exists = device.shell(`[ -f ${remote} ] && echo yes || true`).trim() === "yes";
  let xml = exists ? device.shell(`cat ${remote}`) : "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n</map>\n";
  for (const [name, value] of Object.entries(entries)) {
    const element = preferenceElement(name, value);
    const existing = new RegExp(`^\\s*<(\\w+) name="${escapeRegExp(name)}"[^]*?(/>|</\\1>)$`, "m");
    xml = existing.test(xml) ? xml.replace(existing, `    ${element}`) : xml.replace("</map>", `    ${element}\n</map>`);
  }

  const directory = mkdtempSync(join(tmpdir(), "katari-preferences-"));
  try {
    const local = join(directory, file);
    writeFileSync(local, xml);
    const staged = `/data/local/tmp/${file}`;
    device.forceStop(appId);
    device.push(local, staged);
    // Writing through the existing file keeps the app's ownership and SELinux label on it.
    if (exists) {
      device.shell(`cat ${staged} > ${remote}`);
    } else {
      const owner = device.shell(`stat -c %u:%g /data/data/${appId}`).trim();
      device.shell(`mkdir -p /data/data/${appId}/shared_prefs && cp ${staged} ${remote}`);
      device.shell(`chown -R ${owner} /data/data/${appId}/shared_prefs && restorecon -R /data/data/${appId}/shared_prefs`);
    }
    device.shell(`rm ${staged}`);
  } finally {
    rmSync(directory, { recursive: true, force: true });
  }
}

function preferenceElement(name: string, value: PreferenceValue): string {
  if (typeof value === "string") return `<string name="${name}">${escapeXml(value)}</string>`;
  if (typeof value === "boolean") return `<boolean name="${name}" value="${value}" />`;
  // Every number the pipeline writes is a timestamp, which SharedPreferences stores as a long.
  return `<long name="${name}" value="${value}" />`;
}

function escapeXml(value: string): string {
  return value.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}
