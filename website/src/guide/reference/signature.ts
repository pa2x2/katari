import { codeToTokens } from "shiki";

/** A run of a signature's text in one colour. `style` holds Shiki's light and dark colours; the declared name has none. */
export interface SignatureSpan {
  text: string;
  style?: string;
  name: boolean;
}

/**
 * A run of a signature that stays on one line. A parameter too long for a phone's column has three parts: its name and
 * type, the space before its default value, and the default. Only a phone may break at that space; a wider screen keeps
 * the parameter whole and wraps between pieces instead.
 */
export type SignaturePiece = SignatureSpan[][];

/**
 * A Kotlin declaration such as `fun applySuggestion(input: EntryFilterTextInput, suggestion: EntryFilterSuggestion):
 * EntryFilterTextEdit`, highlighted, with its declared name marked, and cut into pieces. A signature too long for its
 * line breaks only between pieces, the way Kotlin wraps a long declaration: after the opening parenthesis, after each
 * parameter, and before the closing one. One without parameters, such as an interface with its supertype, breaks at the
 * spaces after its name, and any may break between its keywords and its name. A name or a type is never split.
 */
export async function highlightSignature(signature: string): Promise<SignaturePiece[]> {
  const { tokens } = await codeToTokens(signature, {
    lang: "kotlin",
    themes: { light: "github-light-high-contrast", dark: "github-dark-default" },
    defaultColor: false,
  });
  const depth = nestingDepths(signature);
  const name = declaredName(signature, depth);
  const after = name?.end ?? 0;
  const parameters = parameterBreaks(signature, depth, after);
  const breaks = new Set([
    ...spaceBreaks(signature, depth, 0, name?.start ?? 0),
    ...(parameters.size > 0 ? parameters : spaceBreaks(signature, depth, after, signature.length)),
  ]);
  const defaults = defaultBreaks(signature, depth, parameters);
  const cuts = new Set([...breaks, ...defaults, ...[...defaults].map((index) => index + 1), ...(name ? [name.start, name.end] : [])]);

  const pieces: SignaturePiece[] = [[[]]];
  for (const token of tokens.flat()) {
    const style = Object.entries(token.htmlStyle ?? {})
      .map(([property, value]) => `${property}:${value}`)
      .join(";");
    let start = token.offset;
    const end = token.offset + token.content.length;
    while (start < end) {
      let stop = start + 1;
      while (stop < end && !cuts.has(stop)) stop++;
      if (breaks.has(start) && pieces.at(-1)!.at(-1)!.length > 0) pieces.push([[]]);
      if (defaults.has(start) || defaults.has(start - 1)) pieces.at(-1)!.push([]);
      const isName = name !== undefined && start >= name.start && start < name.end;
      pieces.at(-1)!.at(-1)!.push({ text: signature.slice(start, stop), style: isName ? undefined : style, name: isName });
      start = stop;
    }
  }
  return pieces;
}

/** How deep in parentheses and angle brackets each character sits; the brackets themselves count as outside. */
function nestingDepths(signature: string): number[] {
  let level = 0;
  return [...signature].map((char, index) => {
    const arrow = char === ">" && signature[index - 1] === "-";
    if (char === "(" || char === "<") return level++;
    if (char === ")" || (char === ">" && !arrow)) return --level;
    return level;
  });
}

const KEYWORDS = new Set([
  "abstract",
  "class",
  "const",
  "data",
  "enum",
  "fun",
  "interface",
  "object",
  "open",
  "operator",
  "override",
  "sealed",
  "suspend",
  "val",
  "var",
]);

/** The identifier being declared: the last one before the parameters, the type, the value or the type parameters. */
function declaredName(signature: string, depth: number[]): { start: number; end: number } | undefined {
  for (const match of signature.matchAll(/[A-Za-z_]\w*(?=\s*(?:[(:=<]|$))/g)) {
    if (depth[match.index] === 0 && !KEYWORDS.has(match[0])) {
      return { start: match.index, end: match.index + match[0].length };
    }
  }
  return undefined;
}

/**
 * Offsets in the parameter list that follows the name, past any type parameters, where the signature may break onto a
 * new line. A parenthesis further on, such as in a default value, opens no parameter list.
 */
function parameterBreaks(signature: string, depth: number[], after: number): Set<number> {
  const open = signature.indexOf("(", after);
  if (open < 0 || depth[open] !== 0 || !/^(<.*>)?$/.test(signature.slice(after, open))) return new Set();
  const close = depth.findIndex((level, index) => index > open && level === 0 && signature[index] === ")");
  if (close < 0 || close === open + 1) return new Set();
  const breaks = new Set([open + 1, close]);
  for (let index = open + 1; index < close; index++) {
    if (signature[index] === "," && depth[index] === 1) breaks.add(index + 2);
  }
  return breaks;
}

/** About as many characters as a phone's column holds at the signature's phone size. */
const LONG_PARAMETER = 38;

/** The space before the default value of each parameter longer than a phone's column. */
function defaultBreaks(signature: string, depth: number[], parameters: Set<number>): Set<number> {
  const bounds = [...parameters].sort((a, b) => a - b);
  const breaks = new Set<number>();
  for (const [index, start] of bounds.slice(0, -1).entries()) {
    const end = bounds[index + 1]!;
    const assignment = signature.indexOf(" = ", start);
    if (end - start > LONG_PARAMETER && assignment >= 0 && assignment < end && depth[assignment] === 1) {
      breaks.add(assignment);
    }
  }
  return breaks;
}

/** Offsets after each space outside brackets between `from` and `until`. */
function spaceBreaks(signature: string, depth: number[], from: number, until: number): Set<number> {
  const breaks = new Set<number>();
  for (let index = from; index < Math.min(until, signature.length - 1); index++) {
    if (signature[index] === " " && depth[index] === 0) breaks.add(index + 1);
  }
  return breaks;
}
