import compatibility from "../../../../app/extension-compatibility.json";

/**
 * The extension families Katari loads and the library versions of each, from app/extension-compatibility.json. The
 * app's loader is generated from the same file, so the website can't list a version the released app doesn't run.
 */
export interface ExtensionFamily {
  id: string;
  name: string;
  library: string;
  /** `major.minor`, or `major` for libraries numbered that way. */
  versions: string[];
}

export const extensionFamilies: ExtensionFamily[] = compatibility.families;

export function extensionFamily(id: string): ExtensionFamily {
  const family = extensionFamilies.find((candidate) => candidate.id === id);
  if (!family) throw new Error(`app/extension-compatibility.json has no family ${id}`);
  return family;
}

/** Such as `14, 16 or 17`. */
export function versionList(family: ExtensionFamily): string {
  const { versions } = family;
  return versions.length > 1 ? `${versions.slice(0, -1).join(", ")} or ${versions.at(-1)}` : versions[0]!;
}
