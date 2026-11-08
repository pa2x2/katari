import { createHash } from "node:crypto";

/** The id Katari derives for an HTTP source (`EntryHttpSource.generateId`): the first 8 bytes of an MD5, made positive. */
export function sourceId(name: string, language: string, versionId = 1): bigint {
  const digest = createHash("md5").update(`${name.toLowerCase()}/${language}/${versionId}`).digest();
  return digest.readBigUInt64BE(0) & 0x7fffffffffffffffn;
}
