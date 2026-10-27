import type { ShikiTransformer } from "shiki";

/**
 * Reads a snippet written as a diff: a line starting with `- ` is code the change removes, `+ ` code it adds, and two
 * spaces code it keeps. The markers come off before highlighting, so both sides keep their language's colours, and each
 * removed or added line gets that class for Snippet to tint and sign.
 */
export function diffLines(): ShikiTransformer {
  let kinds: ("removed" | "added" | undefined)[] = [];
  return {
    name: "diff-lines",
    preprocess(code) {
      const lines = code.split("\n");
      kinds = lines.map((line) => (line.startsWith("-") ? "removed" : line.startsWith("+") ? "added" : undefined));
      // Snippet trims the code, which takes a kept first line's two spaces with it, so a line may lack its marker.
      return lines.map((line) => (/^[-+ ]( |$)/.test(line) ? line.slice(2) : line)).join("\n");
    },
    line(node, line) {
      const kind = kinds[line - 1];
      if (kind) this.addClassToHast(node, kind);
    },
  };
}
