import changelog from "../../../CHANGELOG.md?raw";

export interface ReleaseNotes {
  /** Such as `1.14.2`; its tag is the same with a leading `v`. */
  version: string;
  date: Date;
  /** The kinds of change in the changelog's order, such as Added or Fixed, each with one sentence per change. */
  sections: { kind: string; changes: string[] }[];
}

/**
 * Every release in CHANGELOG.md, newest first. A release starts with `## [1.14.2] - 2026-10-11`, each kind of change
 * with a heading such as `### 🐛 Fixed`, and each change is one `- ` line.
 */
export function releaseNotes(markdown: string = changelog): ReleaseNotes[] {
  return markdown.split(/^(?=## \[)/m).flatMap((release) => {
    const heading = /^## \[(\d+\.\d+\.\d+)\] - (\d{4}-\d{2}-\d{2})$/m.exec(release);
    if (!heading) return [];
    const sections = release
      .split(/^(?=### )/m)
      .slice(1)
      .map((section) => ({
        // The kind follows an emoji, which the card leaves out.
        kind: section.slice(4, section.indexOf("\n")).replace(/^[^\p{L}]+/u, ""),
        changes: section
          .split("\n")
          .filter((line) => line.startsWith("- "))
          .map((line) => line.slice(2)),
      }))
      .filter((section) => section.changes.length > 0);
    return [{ version: heading[1]!, date: new Date(`${heading[2]}T00:00:00Z`), sections }];
  });
}
