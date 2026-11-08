import changelog from "../../../sources/entry-source-api/CHANGELOG.md?raw";
import catalog from "../../../gradle/libs.versions.toml?raw";
import { REPOSITORY } from "../site/links.ts";

/**
 * The newest SDK release in sources/entry-source-api/CHANGELOG.md, which the extension pages build against in their
 * snippets and link the SDK's source at. A release starts with `## [2.6.0] - 2026-09-06` and is published from the tag
 * `sdk-2.6.0`, so a new SDK release moves the pages along without editing them.
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
  if (!heading) throw new Error("sources/entry-source-api/CHANGELOG.md names no release");
  const tag = `sdk-${heading[1]}`;
  return {
    version: heading[1]!,
    family: `${heading[2]}.${heading[3]}`,
    tag,
    dependency: `com.github.pa2x2.katari:entry-source-api:${tag}`,
  };
}

export const sdk = newestSdk();

/** A directory of the katari repository at the SDK's tag, named by its path in the repository as it is now. */
export function sdkTree(path: string): string {
  return `${REPOSITORY}/tree/${sdk.tag}/${pathAtSdkTag(path)}`;
}

/** A file of the katari repository at the SDK's tag, named by its path in the repository as it is now. */
export function sdkFile(path: string): string {
  return `${REPOSITORY}/blob/${sdk.tag}/${pathAtSdkTag(path)}`;
}

/** The newest SDK release whose tag holds the modules now in `sources/` at the repository's root. */
const LAST_RELEASE_WITH_SOURCES_AT_ROOT = [3, 0, 0];

function pathAtSdkTag(path: string): string {
  const version = sdk.version.split(".").map(Number);
  const differing = version.findIndex((part, index) => part !== LAST_RELEASE_WITH_SOURCES_AT_ROOT[index]);
  const sourcesAtRoot = differing === -1 || version[differing]! < LAST_RELEASE_WITH_SOURCES_AT_ROOT[differing]!;
  return sourcesAtRoot ? path.replace(/^sources\//, "") : path;
}

/** Where the SDK's own interfaces live, for linking one by file name. */
export const SDK_SOURCE = "sources/entry-source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source/entry";

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
