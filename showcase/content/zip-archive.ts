/**
 * Reads members of a zip archive, such as an EPUB, from a file already in memory or in byte ranges from its upstream
 * site, so a cover can be taken from a book without downloading the rest of it. The archive's central directory at its
 * end says where every member sits. Zip64 is not read; no EPUB comes near the 4 GiB it is needed for.
 */
import { inflateRawSync } from "node:zlib";
import { byteRange, cachedRange } from "./download-cache.ts";

interface ZipMember {
  method: number;
  compressedSize: number;
  size: number;
  localHeaderOffset: number;
}

const END_RECORD_SIGNATURE = 0x06054b50;
const END_RECORD_LENGTH = 22;
// The end record is followed by a comment of at most 65535 bytes, so it is found within this many bytes of the end.
const END_RECORD_SEARCH = END_RECORD_LENGTH + 0xffff;
const CENTRAL_HEADER_SIGNATURE = 0x02014b50;
const LOCAL_HEADER_SIGNATURE = 0x04034b50;
const LOCAL_HEADER_LENGTH = 30;
const STORED = 0;
const DEFLATED = 8;

export interface LocalZip {
  /** Uncompressed size of every member, by name. */
  sizes: Map<string, number>;
  read(name: string): Buffer;
}

export interface RemoteZip {
  read(name: string): Promise<Buffer>;
}

export function localZip(archive: Buffer): LocalZip {
  const [start, end] = centralDirectoryRange(archive, 0);
  const members = centralDirectory(archive.subarray(start, end));
  return {
    sizes: new Map([...members].map(([name, member]) => [name, member.size])),
    read(name) {
      const member = memberNamed(members, name);
      const dataStart = member.localHeaderOffset + localHeaderLength(archive.subarray(member.localHeaderOffset));
      return decompress(member, archive.subarray(dataStart, dataStart + member.compressedSize));
    },
  };
}

/** The archive at [url], read in byte ranges that the download cache keeps. */
export async function remoteZip(url: string): Promise<RemoteZip> {
  const tail = await cachedRange(url, `bytes=-${END_RECORD_SEARCH}`);
  const tailStart = tail.size - tail.bytes.length;
  const [start, end] = centralDirectoryRange(tail.bytes, tailStart);
  const directory =
    start >= tailStart
      ? tail.bytes.subarray(start - tailStart, end - tailStart)
      : (await cachedRange(url, byteRange(start, end - 1))).bytes;
  const members = centralDirectory(directory);
  return {
    async read(name) {
      const member = memberNamed(members, name);
      const offset = member.localHeaderOffset;
      const header = await cachedRange(url, byteRange(offset, offset + LOCAL_HEADER_LENGTH - 1));
      const dataStart = offset + localHeaderLength(header.bytes);
      if (member.compressedSize === 0) return Buffer.alloc(0);
      const data = await cachedRange(url, byteRange(dataStart, dataStart + member.compressedSize - 1));
      return decompress(member, data.bytes);
    },
  };
}

/** Where the central directory starts and ends, from [tail], the archive's last bytes starting at [tailStart]. */
function centralDirectoryRange(tail: Buffer, tailStart: number): [number, number] {
  for (let index = tail.length - END_RECORD_LENGTH; index >= 0; index--) {
    if (tail.readUInt32LE(index) !== END_RECORD_SIGNATURE) continue;
    const length = tail.readUInt32LE(index + 12);
    const start = tail.readUInt32LE(index + 16);
    if (start === 0xffffffff || length === 0xffffffff) throw new Error("Zip64 archives are not supported");
    // A comment that happens to contain the signature would point past the record found from the end.
    if (start + length === tailStart + index) return [start, start + length];
  }
  throw new Error("No zip end record found");
}

function centralDirectory(directory: Buffer): Map<string, ZipMember> {
  const members = new Map<string, ZipMember>();
  let index = 0;
  while (index < directory.length) {
    if (directory.readUInt32LE(index) !== CENTRAL_HEADER_SIGNATURE) throw new Error("Corrupt zip central directory");
    const nameLength = directory.readUInt16LE(index + 28);
    const extraLength = directory.readUInt16LE(index + 30);
    const commentLength = directory.readUInt16LE(index + 32);
    members.set(directory.toString("utf8", index + 46, index + 46 + nameLength), {
      method: directory.readUInt16LE(index + 10),
      compressedSize: directory.readUInt32LE(index + 20),
      size: directory.readUInt32LE(index + 24),
      localHeaderOffset: directory.readUInt32LE(index + 42),
    });
    index += 46 + nameLength + extraLength + commentLength;
  }
  return members;
}

/** The length of the local header [header] starts with; its name and extra field may differ from the directory's. */
function localHeaderLength(header: Buffer): number {
  if (header.readUInt32LE(0) !== LOCAL_HEADER_SIGNATURE) throw new Error("Corrupt zip local header");
  return LOCAL_HEADER_LENGTH + header.readUInt16LE(26) + header.readUInt16LE(28);
}

function memberNamed(members: Map<string, ZipMember>, name: string): ZipMember {
  return members.get(name) ?? raise(`The archive has no member ${name}`);
}

function decompress(member: ZipMember, data: Buffer): Buffer {
  switch (member.method) {
    case STORED:
      return data;
    case DEFLATED:
      return inflateRawSync(data);
    default:
      throw new Error(`Zip compression method ${member.method} is not supported`);
  }
}

function raise(message: string): never {
  throw new Error(message);
}
