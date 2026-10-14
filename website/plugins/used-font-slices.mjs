import { readdirSync, readFileSync } from "node:fs";
import { join } from "node:path";

const STYLESHEET = "/@fontsource/shippori-mincho/700.css";
// Each face with the comment Fontsource names it by.
const FACE = /(?:\/\*[^*]*\*\/\s*)?@font-face\s*\{[^}]*\}/g;
// Japanese slices are numbered, such as shippori-mincho-113-700-normal.woff2; the Latin faces are named.
const SLICE = /-\d+-\d+-normal\.woff2/;

/**
 * Shippori Mincho's stylesheet declares about 120 numbered slices of Japanese besides its Latin faces: 147 KB of CSS
 * that every page loads before it can draw, for the handful of Japanese characters the site sets in the font. This keeps
 * the Latin faces and only the slices holding a character that the site's sources use and the Latin faces don't cover.
 * Some slices list Latin characters too, but the Latin faces are declared after them, so the browser takes those from
 * the Latin faces anyway.
 */
export function usedFontSlices(sourceDirectory) {
  return {
    name: "used-font-slices",
    enforce: "pre",
    transform(code, id) {
      if (!id.split("?")[0].endsWith(STYLESHEET)) return;
      const sources = textFiles(sourceDirectory);
      for (const file of sources) this.addWatchFile(file);
      const faces = [...code.matchAll(FACE)].map(([text]) => ({ text, ranges: unicodeRanges(text), slice: SLICE.test(text) }));
      const latin = faces.filter((face) => !face.slice);
      const used = new Set(sources.flatMap((file) => [...readFileSync(file, "utf8")].map((char) => char.codePointAt(0))));
      const needed = [...used].filter((point) => !latin.some((face) => covers(face.ranges, point)));
      return faces
        .filter((face) => face.slice && !needed.some((point) => covers(face.ranges, point)))
        .reduce((css, face) => css.replace(face.text, ""), code);
    },
  };
}

function textFiles(directory) {
  return readdirSync(directory, { recursive: true })
    .filter((name) => /\.(astro|ts|css|md)$/.test(name))
    .map((name) => join(directory, name));
}

/** Without a unicode-range, a face covers every character. */
function unicodeRanges(face) {
  const declared = /unicode-range:([^;]+)/.exec(face);
  if (!declared) return [[0, 0x10ffff]];
  return declared[1].split(",").map((range) => {
    const [from, to = from] = range.trim().slice(2).split("-");
    return [parseInt(from, 16), parseInt(to, 16)];
  });
}

function covers(ranges, point) {
  return ranges.some(([from, to]) => from <= point && point <= to);
}
