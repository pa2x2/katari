/**
 * HTML for one line of a changelog: `code` spans and [links](url), everything else as text. `link` turns a link's
 * address into the attributes of its `<a>`, so a page decides where links lead and can reject one.
 */
export function inlineMarkdown(line: string, link: (url: string) => string): string {
  return line
    .split(/(`[^`]+`)/)
    .map((part, index) =>
      // split() puts the captured code spans at the odd indices.
      index % 2 === 1
        ? `<code>${escape(part.slice(1, -1))}</code>`
        : escape(part).replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_, text: string, url: string) => `<a ${link(url)}>${text}</a>`),
    )
    .join("");
}

function escape(text: string): string {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
