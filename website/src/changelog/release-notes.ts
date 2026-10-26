import changelog from "../../../CHANGELOG.md?raw";

export interface Release {
  /** Such as `1.14.2`. */
  version: string;
  /** Undefined while the heading says `Unreleased`. */
  date?: Date;
  /** Lines of a `> ` quote between the heading and the first kind of change, such as a pointer to an upgrade guide. */
  notes: string[];
  /** The kinds of change in the changelog's order, such as Added or Fixed, each with one sentence per change. */
  sections: { kind: string; changes: string[] }[];
}

export interface ReleaseNotes extends Release {
  date: Date;
}

/**
 * Every release in a changelog written like CHANGELOG.md and SDK-CHANGELOG.md, newest first. A release starts with
 * `## [1.14.2] - 2026-10-11` or `## [3.0.0] - Unreleased`, each kind of change with a heading such as `### 🐛 Fixed`,
 * and each change is one `- ` line.
 */
export function releases(markdown: string): Release[] {
  return markdown.split(/^(?=## \[)/m).flatMap((release) => {
    const heading = /^## \[(\d+\.\d+\.\d+)\] - (?:(\d{4}-\d{2}-\d{2})|Unreleased)$/m.exec(release);
    if (!heading) return [];
    const [intro, ...kinds] = release.split(/^(?=### )/m);
    const sections = kinds
      .map((section) => ({
        // The kind follows an emoji, which the site leaves out.
        kind: section.slice(4, section.indexOf("\n")).replace(/^[^\p{L}]+/u, ""),
        changes: section
          .split("\n")
          .filter((line) => line.startsWith("- "))
          .map((line) => line.slice(2)),
      }))
      .filter((section) => section.changes.length > 0);
    const notes = intro!
      .split("\n")
      .filter((line) => line.startsWith("> "))
      .map((line) => line.slice(2));
    const date = heading[2] ? new Date(`${heading[2]}T00:00:00Z`) : undefined;
    return [{ version: heading[1]!, date, notes, sections }];
  });
}

/** Katari's released versions in CHANGELOG.md, newest first. */
export function releaseNotes(markdown: string = changelog): ReleaseNotes[] {
  return releases(markdown).filter((release): release is ReleaseNotes => release.date !== undefined);
}
