import changelog from "../../../../SDK-CHANGELOG.md?raw";
import catalog from "../../../../gradle/libs.versions.toml?raw";
import { REPOSITORY } from "../../site/links.ts";

/**
 * The newest SDK release in SDK-CHANGELOG.md, which the extension pages build against in their snippets and link the
 * SDK's source at. A release starts with `## [2.6.0] - 2026-09-06` and is published from the tag `sdk-2.6.0`, so a
 * new SDK release moves the pages along without editing them.
 */
export interface Sdk {
  /** Such as `2.6.0`. */
  version: string;
  /** Major and minor, such as `2.6`: the part of an extension's versionName that Katari reads. */
  family: string;
  tag: string;
  /** The Gradle dependency an extension compiles against. */
  dependency: string;
}

export function newestSdk(markdown: string = changelog): Sdk {
  const heading = /^## \[((\d+)\.(\d+)\.\d+)\]/m.exec(markdown);
  if (!heading) throw new Error("SDK-CHANGELOG.md names no release");
  const tag = `sdk-${heading[1]}`;
  return {
    version: heading[1]!,
    family: `${heading[2]}.${heading[3]}`,
    tag,
    dependency: `com.github.pa2x2.katari:entry-source-api:${tag}`,
  };
}

export const sdk = newestSdk();

/** A directory of the katari repository at the SDK's tag. */
export function sdkTree(path: string): string {
  return `${REPOSITORY}/tree/${sdk.tag}/${path}`;
}

/** A file of the katari repository at the SDK's tag. */
export function sdkFile(path: string): string {
  return `${REPOSITORY}/blob/${sdk.tag}/${path}`;
}

/** Where the SDK's own interfaces live, for linking one by file name. */
export const SDK_SOURCE = "entry-source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/entry";

/**
 * A library's version in the version catalog the website is built with, which is the one Katari runs, so the pages name
 * the versions extensions meet at runtime without being edited when a library is updated.
 */
export function libraryVersion(name: string, toml: string = catalog): string {
  const versions = toml.slice(toml.indexOf("[versions]"), toml.indexOf("\n[", toml.indexOf("[versions]") + 1));
  const line = new RegExp(`^${name.replace(/[.-]/g, "\\$&")}\\s*=\\s*"([^"]+)"`, "m").exec(versions);
  if (!line) throw new Error(`gradle/libs.versions.toml has no version ${name}`);
  return line[1]!;
}
