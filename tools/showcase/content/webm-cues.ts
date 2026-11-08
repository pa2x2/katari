/**
 * Where a WebM keeps each moment of its video, from the index (Cues) it carries: the byte offset of the cluster that
 * starts at each indexed time. A WebM is EBML, whose elements are an ID and a size, each a variable-length integer
 * whose leading zero bits say how many bytes it takes.
 */

export interface CuePoint {
  seconds: number;
  /** Offset in the file of the cluster the cue points at. */
  offset: number;
}

const SEGMENT = 0x18538067;
const SEEK_HEAD = 0x114d9b74;
const SEEK = 0x4dbb;
const SEEK_ID = 0x53ab;
const SEEK_POSITION = 0x53ac;
const INFO = 0x1549a966;
const TIMESTAMP_SCALE = 0x2ad7b1;
const CUES = 0x1c53bb6b;
const CUE_POINT = 0xbb;
const CUE_TIME = 0xb3;
const CUE_TRACK_POSITIONS = 0xb7;
const CUE_CLUSTER_POSITION = 0xf1;
const DEFAULT_TIMESTAMP_SCALE_NS = 1_000_000;

interface Element {
  id: number;
  /** Where its content starts and ends in the buffer it was read from. */
  start: number;
  end: number;
}

/** The cue points of the WebM [read] returns byte ranges of, as `[start, end)`. */
export async function cuePoints(read: (start: number, end: number) => Promise<Buffer>): Promise<CuePoint[]> {
  // The header, the segment's start, its seek head and its info come first, well within this.
  const head = await read(0, 64 * 1024);
  const segment = elements(head, 0, head.length).find((element) => element.id === SEGMENT);
  if (segment === undefined) throw new Error("No WebM segment found");
  let cuesPosition: number | undefined;
  let timestampScale = DEFAULT_TIMESTAMP_SCALE_NS;
  for (const element of elements(head, segment.start, head.length)) {
    if (element.id === SEEK_HEAD) {
      for (const seek of children(head, element, SEEK)) {
        const id = child(head, seek, SEEK_ID);
        const position = child(head, seek, SEEK_POSITION);
        if (id !== undefined && position !== undefined && unsigned(head, id) === CUES) cuesPosition = unsigned(head, position);
      }
    } else if (element.id === INFO) {
      const scale = child(head, element, TIMESTAMP_SCALE);
      if (scale !== undefined) timestampScale = unsigned(head, scale);
    }
  }
  if (cuesPosition === undefined) throw new Error("The WebM's seek head does not say where its cues are");
  // Positions in the segment count from the start of its content.
  const cuesStart = segment.start + cuesPosition;
  const cuesHeader = await read(cuesStart, cuesStart + 12);
  const [cues] = elements(cuesHeader, 0, cuesHeader.length, true);
  if (cues?.id !== CUES) throw new Error("No WebM cues where the seek head points");
  const content = await read(cuesStart + cues.start, cuesStart + cues.end);
  return children(content, { id: CUES, start: 0, end: content.length }, CUE_POINT).map((point) => {
    const time = child(content, point, CUE_TIME);
    const positions = children(content, point, CUE_TRACK_POSITIONS)[0];
    const cluster = positions && child(content, positions, CUE_CLUSTER_POSITION);
    if (time === undefined || cluster === undefined) throw new Error("A WebM cue point has no time or cluster");
    return {
      seconds: (unsigned(content, time) * timestampScale) / 1e9,
      offset: segment.start + unsigned(content, cluster),
    };
  });
}

/**
 * The elements one after another in [buffer] from [start] to [end]. Only the first is read when [headerOnly], whose
 * content may lie past the buffer.
 */
function elements(buffer: Buffer, start: number, end: number, headerOnly = false): Element[] {
  const found: Element[] = [];
  let index = start;
  while (index < end) {
    const id = variableInteger(buffer, index, true);
    const size = id && variableInteger(buffer, index + id.length, false);
    // A header cut off by the end of what was read.
    if (id === undefined || size === undefined) break;
    const contentStart = index + id.length + size.length;
    // An unknown size, as live-written segments use, runs to the end of what holds it.
    const contentEnd = size.unknown ? end : contentStart + size.value;
    found.push({ id: id.value, start: contentStart, end: contentEnd });
    if (headerOnly || contentEnd > end) break;
    index = contentEnd;
  }
  return found;
}

function children(buffer: Buffer, parent: Element, id: number): Element[] {
  return elements(buffer, parent.start, Math.min(parent.end, buffer.length)).filter((element) => element.id === id);
}

function child(buffer: Buffer, parent: Element, id: number): Element | undefined {
  return children(buffer, parent, id)[0];
}

function unsigned(buffer: Buffer, element: Element): number {
  let value = 0;
  for (let index = element.start; index < element.end; index++) value = value * 256 + buffer[index]!;
  return value;
}

/**
 * An EBML variable-length integer, or nothing when [buffer] ends inside it. An ID keeps its length marker bits, a size
 * drops them.
 */
function variableInteger(
  buffer: Buffer,
  index: number,
  keepMarker: boolean,
): { value: number; length: number; unknown: boolean } | undefined {
  if (index >= buffer.length) return undefined;
  const first = buffer[index]!;
  let length = 1;
  while (length <= 8 && (first & (0x80 >> (length - 1))) === 0) length++;
  if (length > 8) throw new Error("Corrupt EBML variable-length integer");
  if (index + length > buffer.length) return undefined;
  let value = keepMarker ? first : first & (0xff >> length);
  let allOnes = value === 0xff >> length;
  for (let offset = 1; offset < length; offset++) {
    value = value * 256 + buffer[index + offset]!;
    allOnes &&= buffer[index + offset] === 0xff;
  }
  return { value, length, unknown: !keepMarker && allOnes };
}
